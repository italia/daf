import akka.actor.ActorSystem
import play.api.libs.ws.WSClient

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

package it.gov.daf.common.sso.client{

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import it.gov.daf.common.utils.WebServiceUtil
  import play.api.libs.json._
  import play.api.libs.ws.WSClient
  import play.api.libs.ws.ahc.AhcWSClient
  import play.api.mvc.Cookie

  import scala.concurrent.Future

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.ExplicitImplicitTypes",
      "org.wartremover.warts.Overloading",
      "org.wartremover.warts.StringPlusAny",
      "org.wartremover.warts.Throw"
    )
  )
  object SsoServiceClient {

    import scala.concurrent.ExecutionContext.Implicits._

    private implicit val system = ActorSystem()
    private implicit val materializer = ActorMaterializer()

    private implicit val cookieReads = Json.reads[Cookie]


    def registerInternal(host:String, username:String, password:String):Future[String]= {

      val wsClient = AhcWSClient()
      registerInternal(host,username,password,wsClient)
        .andThen { case _ => wsClient.close() }
        .andThen { case _ => system.terminate() }

    }


    def retriveCookieInternal(host:String, username:String,appName:String):Future[Cookie] =  {

      val wsClient = AhcWSClient()
      retriveCookieInternal(host,username,appName,wsClient)
        .andThen { case _ => wsClient.close() }
        .andThen { case _ => system.terminate() }

    }


    def registerInternal(host:String, username:String, password:String, wsClient:WSClient):Future[String]= {

      val params = WebServiceUtil.buildEncodedQueryString(Map("username"->username,"password"->password))
      val url =  host+"/sso-manager/internal/register/"+params

      wsClient.url(url).get().map{ response =>

        if(response.status != 200)
          throw new Exception("User internal registering failed (http status code"+response.status+")")

        response.body

      }

    }


    def retriveCookieInternal(host:String, username:String,appName:String,wsClient:WSClient):Future[Cookie] =  {

      val userParam = WebServiceUtil.buildEncodedQueryString( Map("username"->username) )
      val url =  host+"/sso-manager/internal/retriveCookie/"+appName+userParam

      wsClient.url(url).get().map{ response =>

        if(response.status != 200)
          throw new Exception("Internal user cookie retrive failed (http status code"+response.status+")")

        val json = Json.parse(response.body)
        val cookieFromJson: JsResult[Cookie] = Json.fromJson[Cookie](json)

        cookieFromJson match {
          case JsSuccess(cookie: Cookie, path: JsPath) => cookie
          case e: JsError => throw new Exception("Malformed response:"+response.body)
        }

      }

    }

  }

}
