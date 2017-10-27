/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.gov.daf.common.sso.common

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.Future

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.StringPlusAny"
  )
)
class SecuredInvocationManager(_loginClient:LoginClient) {

  import scala.concurrent.ExecutionContext.Implicits._

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val sslconfig = new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build
  private val loginClient=_loginClient

  private def callService( wsClient:AhcWSClient, loginInfo:LoginInfo, serviceFetch:(String,AhcWSClient)=> Future[WSResponse]):Future[WSResponse] = {

    println("callService ("+loginInfo+")")

    val cookieOpt = getCacheWrapper.getCookie(loginInfo.appName,loginInfo.user)

    if( cookieOpt.isEmpty )

      loginClient.login(loginInfo, wsClient).flatMap { cookie =>

        getCacheWrapper.putCookie(loginInfo.appName,loginInfo.user,cookie)

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
        getCacheWrapper.deleteCookie(loginInfo.appName,loginInfo.user)
        callService(wsClient,loginInfo,serviceFetch).map(_.json)
          .andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }
      }else
        Future{ response.json }

    }

  }


  // default cache wrapper only contain session per 30 minutes
  private def getCacheWrapper = if(CacheWrapper.isInitialized)
                                  CacheWrapper.instance
                                else
                                  CacheWrapper.init(30L,0L)


}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Null"
  )
)
object SecuredInvocationManager{

  private var _instance : SecuredInvocationManager = null

  def init(loginClient:LoginClient):SecuredInvocationManager = {
    if (_instance == null) {
      _instance = new SecuredInvocationManager(loginClient: LoginClient)
      _instance
    }else if(_instance.loginClient == loginClient )
      _instance
    else
      throw new Exception("SecuredInvocationManager is already initialized with different parameters")

  }

  def instance():SecuredInvocationManager = {

    if(_instance != null)
      _instance
    else
      throw new Exception("SecuredInvocationManager not initialized")
  }

}


