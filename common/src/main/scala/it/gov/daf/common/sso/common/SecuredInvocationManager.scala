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

import java.io.{PrintWriter, StringWriter}
import com.google.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.Logger

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Nothing"

  )
)
@Singleton
class SecuredInvocationManager @Inject()(loginClient:LoginClient, cacheWrapper: CacheWrapper, wsCli: WSClient ) {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  type ServiceFetch = (String,WSClient)=> Future[WSResponse]

  private def callService(loginInfo:LoginInfo, serviceFetch:ServiceFetch):Future[WSResponse] = {

    Logger.logger.debug(s"callService ($loginInfo)")

    val cookieOpt = cacheWrapper.getCookie(loginInfo.appName,loginInfo.user)

    if( cookieOpt.isEmpty )

      loginClient.login(loginInfo, wsCli).flatMap { cookie =>

        cacheWrapper.putCookie(loginInfo.appName,loginInfo.user,cookie)

        val cookieString = cookie.name+"="+cookie.value
        serviceFetch(cookieString, wsCli)/*.map{ response =>
          Logger.logger.debug(s"RESPONSE1 ($loginInfo):${response.body}")
          response
        }*/

      }

    else {

      val cookieString = cookieOpt.get.name+"="+cookieOpt.get.value
      serviceFetch(cookieString, wsCli)/*.map { response =>
        Logger.logger.debug(s"RESPONSE2 ($loginInfo): ${response.body}")
        response
      }*/

    }

  }

  def manageRestServiceCall( loginInfo:LoginInfo, serviceFetch:ServiceFetch, acceptableHttpCodes:Int* ) : Future[Either[String,JsValue]] = {

    tryManageRestServiceCall ( loginInfo, serviceFetch, acceptableHttpCodes:_* ) map {
      case Success(s) => Right(s)
      case Failure(e) =>
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        Left(sw.toString)
    }

  }

  private def tryManageRestServiceCall( loginInfo:LoginInfo, serviceFetch:ServiceFetch, acceptableHttpCodes:Int* ) : Future[Try[JsValue]] = {

    manageServiceCall(loginInfo,serviceFetch) map { response =>
      Try{
        if( acceptableHttpCodes.contains(response.status) )
          response.json
        else
          throw new Exception(s"Error while invoking the service. Unespected http code returned: ${response.status}")
      }
    }

  }


  def manageServiceCall( loginInfo:LoginInfo, serviceFetch:ServiceFetch ) : Future[WSResponse] = {

    Logger.logger.debug(s"manageServiceCall ($loginInfo)")

    callService(loginInfo,serviceFetch) flatMap {response =>

      Logger.logger.debug(s"RESPONSE STATUS(Applicazione:${loginInfo.appName}): ${response.status}")
      Logger.logger.debug(s"RESPONSE BODY (Applicazione:${loginInfo.appName}) ${response.body}")

      if(response.status == 401){
        Logger.logger.warn("Unauthorized!!")
        cacheWrapper.deleteCookie(loginInfo.appName,loginInfo.user)
        callService(loginInfo,serviceFetch)
      }else
        Future{ response }

    }

  }



}



