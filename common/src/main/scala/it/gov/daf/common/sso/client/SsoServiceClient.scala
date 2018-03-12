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

package it.gov.daf.common.sso.client


  import it.gov.daf.common.utils.WebServiceUtil
  import play.api.libs.json._
  import play.api.libs.ws.WSClient
  import play.api.mvc.Cookie

  import scala.concurrent.Future
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.ExplicitImplicitTypes",
      "org.wartremover.warts.Throw"
    )
  )
  object SsoServiceClient {


    private implicit val cookieReads = Json.reads[Cookie]


    def registerInternal(host:String, username:String, password:String, wsClient:WSClient):Future[String]= {

      val params = WebServiceUtil.buildEncodedQueryString(Map("username"->username,"password"->password))
      val url =  s"$host/sso-manager/internal/register/$params"

      wsClient.url(url).get().map{ response =>

        if(response.status != 200)
          throw new Exception(s"User internal registering failed (http status code ${response.status})")

        response.body

      }

    }


    def retriveCookieInternal(host:String, username:String,appName:String,wsClient:WSClient):Future[Cookie] =  {

      val userParam = WebServiceUtil.buildEncodedQueryString( Map("username"->username) )
      val url =  s"$host/sso-manager/internal/retriveCookie/$appName$userParam"

      wsClient.url(url).get().map{ response =>

        if(response.status != 200)
          throw new Exception(s"Internal user cookie retrive failed (http status code ${response.status})")

        val json = Json.parse(response.body)
        val cookieFromJson: JsResult[Cookie] = Json.fromJson[Cookie](json)

        cookieFromJson match {
          case JsSuccess(cookie: Cookie, path: JsPath) => cookie
          case e: JsError => throw new Exception(s"Malformed response:${response.body}")
        }

      }

    }



  }


