package it.gov.daf.play.security

import java.net.URLEncoder

import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Cookie

import scala.concurrent.{ExecutionContext, Future}
import protocol._

object SingleSignOnClient{
  def apply(host: String)(implicit ws: WSClient, ex: ExecutionContext) =
    new SingleSignOnClient(host)(ws, ex)
}

class SingleSignOnClient(host: String)(implicit ws: WSClient, ex: ExecutionContext) {

  private def toQueryString(params: Map[String,Any]): String = {
    val encoded = for {
      (name, value) <- params if value != None
      encodedValue = value match {
        case Some(x)         => URLEncoder.encode(x.toString, "UTF8")
        case x               => URLEncoder.encode(x.toString, "UTF8")
      }
    } yield name + "=" + encodedValue

    encoded.mkString("?", "&", "")
  }

  private def filterNon200(wSResponse: WSResponse): String =
    if (wSResponse.status != 200) throw new Throwable(s"Failed with status ${wSResponse.status}")
    else wSResponse.body

  def registerUser(username: String, password: String): Future[String] = {
    val params = Map("username" -> username, "password" -> password)
    val servicePath = "/sso-manager/internal/register/"
    val url = s"$host$servicePath${toQueryString(params)}"
    ws.url(url).get().map(filterNon200)
  }

  private implicit val cookieReads = Json.reads[Cookie]

  def createCookie(credentials: ServiceCredentials) = retrieveCookie(credentials)

  def retrieveCookie(credentials: ServiceCredentials): Future[Cookie] = {
    val params = Map("username" -> credentials.username)
    val servicePath = s"/sso-manager/internal/retriveCookie/${credentials.appName}"
    val url = s"$host$servicePath${toQueryString(params)}"
    ws.url(url).get().map(filterNon200)
        .map{ body =>
          val cookie = Json.fromJson[Cookie](Json.parse(body))
          cookie match {
            case JsSuccess(c, _) => c
            case e: JsError => throw new Throwable(s"malformed response $body")
          }
        }
  }

}
