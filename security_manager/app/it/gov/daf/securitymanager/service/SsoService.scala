package it.gov.daf.securitymanager.service

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.securitymanager.service.utilities.WebServiceUtil
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.Future

object SsoService  {
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def registerInternal( username:String, password:String ):Future[String]= {

    val params = WebServiceUtil.buildEncodedQueryString(Map("username"->username,"password"->password))
    val wsClient = AhcWSClient()
    val url =  "http://127.0.0.1/sso-manager/internal/register/"+params
    wsClient.url(url).get().map({ response =>

      if(response.status != 200)
        throw new Exception("User internal registering failed (http status code"+response.status+")")

      response.body

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }


  }


  def retriveCookieInternal(username:String,appName:String):Future[String] =  {

    val params = WebServiceUtil.buildEncodedQueryString( Map("username"->username) )
    val wsClient = AhcWSClient()
    val url =  "http://127.0.0.1/sso-manager/internal/retriveCookie/"+appName+params
    wsClient.url(url).get().map({ response =>

      if(response.status != 200)
        throw new Exception("User internal registering failed (http status code"+response.status+")")

      response.body

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


}
