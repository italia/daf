package it.gov.daf.sso.common

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.catalogmanager.utilities.ConfigReader
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
  private val cacheWrapper = CacheWrapper.init(ConfigReader.cookieExpiration,0L)

  private def callService( wsClient:AhcWSClient, loginInfo:LoginInfo, serviceFetch:(String,AhcWSClient)=> Future[WSResponse]):Future[WSResponse] = {

    val cookieOpt = cacheWrapper.getCookie(loginInfo.appName,loginInfo.user)

    if( cookieOpt.isEmpty )

      _loginClient.login(loginInfo, wsClient).flatMap { cookie =>

        cacheWrapper.putCookie(loginInfo.appName,loginInfo.user,cookie)

        serviceFetch(cookie, wsClient).map({ response =>
          println("RESPONSE:"+response.json)
          response
        }).andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }

      }

    else

      serviceFetch( cookieOpt.get, wsClient).map{ response =>
        if(response.status == 200)
          println("RESPONSE:"+response.json)
        response
      }

  }


  def manageServiceCall( loginInfo:LoginInfo, serviceFetch:(String,AhcWSClient)=> Future[WSResponse] ) : Future[JsValue] = {

    val wsClient = AhcWSClient(sslconfig)


    callService(wsClient,loginInfo,serviceFetch) flatMap {response =>

      if(response.status == 401){
        println("Unauthorized!!")
        cacheWrapper.deleteCookie(loginInfo.appName,loginInfo.user)
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


