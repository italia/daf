package it.gov.daf.sso.common

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.Future

class SecuredInvocationManager(loginClient:LoginClient) {

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  private val sslconfig = new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build
  private val _loginClient=loginClient


  private def callService( wsClient:AhcWSClient, loginInfo:LoginInfo, serviceFetch:(String,AhcWSClient)=> Future[WSResponse]):Future[WSResponse] = {

    println("callService ("+loginInfo+")")

    val cookieOpt = CacheWrapper.instance.getCookie(loginInfo.appName,loginInfo.user)

    if( cookieOpt.isEmpty )

      _loginClient.login(loginInfo, wsClient).flatMap { cookie =>

        CacheWrapper.instance.putCookie(loginInfo.appName,loginInfo.user,cookie)

        serviceFetch(cookie, wsClient).map({ response =>
          println("RESPONSE1 ("+loginInfo+"):"+response.body)
          response
        }).andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }

      }

    else

      serviceFetch( cookieOpt.get, wsClient).map{ response =>
        println("RESPONSE2 ("+loginInfo+"):"+response.body)
        response
      }

  }


  def manageServiceCall( loginInfo:LoginInfo, serviceFetch:(String,AhcWSClient)=> Future[WSResponse] ) : Future[JsValue] = {

    val wsClient = AhcWSClient(sslconfig)

    println("manageServiceCall ("+loginInfo+")")

    callService(wsClient,loginInfo,serviceFetch) flatMap {response =>

      println("RESPONSE STATUS("+loginInfo+"):"+response.status)
      println("RESPONSE BODY ("+loginInfo+")"+response.body)

      if(response.status == 401){
        println("Unauthorized!!")
        CacheWrapper.instance.deleteCookie(loginInfo.appName,loginInfo.user)
        callService(wsClient,loginInfo,serviceFetch).map(_.json)
          .andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }
      }else
        Future{ response.json }

    }

  }

}


object SecuredInvocationManager{

  private var _instance : SecuredInvocationManager = null

  def instance(loginClient:LoginClient) = {
    if (_instance == null)
      _instance = new SecuredInvocationManager(loginClient:LoginClient)
    _instance
  }

}


