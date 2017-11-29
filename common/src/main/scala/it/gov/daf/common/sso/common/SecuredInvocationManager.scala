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
import com.google.inject.{Inject, Singleton}
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.OptionPartial"
  )
)
@Singleton
class SecuredInvocationManager @Inject()(loginClient:LoginClient, cacheWrapper: CacheWrapper, wsCli: WSClient ) {

  import scala.concurrent.ExecutionContext.Implicits._

  /*
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val sslconfig = new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build
  */

  type ServiceFetch = (String,WSClient)=> Future[WSResponse]

  private def callService(loginInfo:LoginInfo, serviceFetch:ServiceFetch):Future[WSResponse] = {

    println(s"callService ($loginInfo)")

    val cookieOpt = cacheWrapper.getCookie(loginInfo.appName,loginInfo.user)

    if( cookieOpt.isEmpty )

      loginClient.login(loginInfo, wsCli).flatMap { cookie =>

        cacheWrapper.putCookie(loginInfo.appName,loginInfo.user,cookie)

        val cookieString = cookie.name+"="+cookie.value
        serviceFetch(cookieString, wsCli).map{ response =>
          println(s"RESPONSE1 ($loginInfo):${response.body}")
          response
        }

      }

    else {

      val cookieString = cookieOpt.get.name+"="+cookieOpt.get.value
      serviceFetch(cookieString, wsCli).map { response =>
        println(s"RESPONSE2 ($loginInfo): ${response.body}")
        response
      }

    }

  }


  def manageServiceCall( loginInfo:LoginInfo, serviceFetch:ServiceFetch ) : Future[JsValue] = {

    //val wsClient = AhcWSClient(sslconfig)

    println(s"manageServiceCall ($loginInfo)")

    callService(loginInfo,serviceFetch) flatMap {response =>

      println(s"RESPONSE STATUS($loginInfo): ${response.status}")
      println(s"RESPONSE BODY ($loginInfo) ${response.body}")

      if(response.status == 401){
        println("Unauthorized!!")
        cacheWrapper.deleteCookie(loginInfo.appName,loginInfo.user)
        callService(loginInfo,serviceFetch).map(_.json)
      }else
        Future{ response.json }

    }

  }

/*
  // default cache wrapper only contain session per 30 minutes
  private def getCacheWrapper = if(CacheWrapper.isInitialized)
                                  CacheWrapper.instance
                                else
                                  CacheWrapper.init(30L,0L)
*/

}
/*
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

}*/


