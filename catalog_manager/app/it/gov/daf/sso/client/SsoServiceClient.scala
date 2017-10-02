package it.gov.daf.sso.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

object SsoServiceClient {

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()


  def registerInternal( username:String, password:String ):Future[String]= {

    val wsClient = AhcWSClient()
    SsoServiceClientBase.registerInternal(username,password,wsClient)
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


  def retriveCookieInternal(username:String,appName:String):Future[String] =  {

    val wsClient = AhcWSClient()
    SsoServiceClientBase.retriveCookieInternal(username,appName,wsClient)
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


}
