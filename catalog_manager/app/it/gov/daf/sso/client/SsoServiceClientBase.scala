package it.gov.daf.sso.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.catalogmanager.utilities.{ConfigReader, WebServiceUtil}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

object SsoServiceClientBase  {
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()


  def registerInternal( username:String, password:String, wsClient:WSClient ):Future[String]= {

    val params = WebServiceUtil.buildEncodedQueryString(Map("username"->username,"password"->password))
    val url =  ConfigReader.securityManHost+"/sso-manager/internal/register/"+params

    wsClient.url(url).get().map{ response =>

      if(response.status != 200)
        throw new Exception("User internal registering failed (http status code"+response.status+")")

      response.body

    }

  }


  def retriveCookieInternal(username:String,appName:String,wsClient:WSClient):Future[String] =  {

    val userParam = WebServiceUtil.buildEncodedQueryString( Map("username"->username) )
    val url =  ConfigReader.securityManHost+"/sso-manager/internal/retriveCookie/"+appName+userParam

    wsClient.url(url).get().map{ response =>

      if(response.status != 200)
        throw new Exception("Internal user cookie retrive failed (http status code"+response.status+")")

      response.body

    }

  }


}
