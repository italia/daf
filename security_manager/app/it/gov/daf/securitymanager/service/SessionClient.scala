package it.gov.daf.securitymanager.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.securitymanager.service.utilities.{ConfigReader, WebServiceUtil}
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import scala.collection.mutable
import scala.concurrent.Future


object SessionClient {

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  private val sslconfig = new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build
  private val sessionCookies:mutable.Map[String,String]= mutable.Map[String,String]()



  private def callService( wsClient:AhcWSClient, payload:JsValue, appName:String, serviceCall:(AhcWSClient,JsValue,String)  => Future[WSResponse]):Future[WSResponse] = {


    if( sessionCookies.get(appName).isEmpty )

      LoginClient.login(ConfigReader.ipaUser,ConfigReader.ipaUserPwd,wsClient,appName).flatMap { cookie =>

        sessionCookies.put(appName,cookie)

        serviceCall(wsClient, payload, cookie ).map({ response =>
          println("RESPONSE:"+response.json)
          response
        }).andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }

      }

    else

      serviceCall(wsClient, payload, sessionCookies.get(appName).get ).map{ response =>
        if(response.status == 200)
          println("RESPONSE:"+response.json)
        response
      }


  }


  def manageServiceCall(payload:JsValue, appName:String, serviceCall:(AhcWSClient,JsValue,String)  => Future[WSResponse]) : Future[JsValue] = {

    val wsClient = AhcWSClient(sslconfig)

    callService(wsClient,payload,appName,serviceCall) flatMap {response1 =>

      if(response1.status == 401){
        println("Unauthorized!!")
        sessionCookies.remove(appName)
        callService(wsClient,payload,appName,serviceCall).map({response2 => response2.json})
          .andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }
      }else
         Future{ response1.json }

    }

  }


}