package it.gov.daf.sso

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Singleton
import it.gov.daf.common.sso.common.{LoginClient, LoginInfo}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSCookie, WSResponse}
import play.api.mvc.Cookie

import scala.concurrent.Future

@Singleton
class LoginClientLocal() extends LoginClient {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits._

  //private val CKAN_URL = "http://localhost:5000"

  private val CKAN = "ckan"
  private val FREE_IPA = "freeIPA"
  private val SUPERSET = "superset"
  private val METABASE = "metabase"
  private val JUPYTER = "jupyter"
  private val GRAFANA = "grafana"

  private val CKAN_URL = ConfigReader.ckanHost
  private val IPA_URL = ConfigReader.ipaUrl
  private val SUPERSET_URL = ConfigReader.supersetUrl
  private val METABASE_URL = ConfigReader.metabaseUrl
  private val JUPYTER_URL = ConfigReader.jupyterUrl
  private val GRAFANA_URL = ConfigReader.grafanaUrl
  private val IPA_APP_ULR = IPA_URL + "/ipa"
  private val IPA_LOGIN_ULR = IPA_URL + "/ipa/session/login_password"

  def login(loginInfo: LoginInfo, wsClient: WSClient): Future[Cookie] = {

    loginInfo.appName match {
      case LoginClientLocal.CKAN => loginCkan(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.FREE_IPA => loginIPA(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.SUPERSET => loginSuperset(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.METABASE => loginMetabase(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.JUPYTER => loginJupyter(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.GRAFANA => loginGrafana(loginInfo.user, loginInfo.password, wsClient)
      case _ => throw new Exception("Unexpeted exception: application name not found")
    }

  }

  def loginFE(loginInfo: LoginInfo, wsClient: WSClient): Future[Seq[Cookie]] = {

    loginInfo.appName match {
      case LoginClientLocal.CKAN => loginCkan(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.FREE_IPA => loginIPA(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.SUPERSET => loginSuperset(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.METABASE => loginMetabase(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.JUPYTER => loginJupyter(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.GRAFANA => loginGrafanaFE(loginInfo.user, loginInfo.password, wsClient)
      case _ => throw new Exception("Unexpeted exception: application name not found")
    }

  }


  private def loginCkan(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val login = s"login=$userName&password=${URLEncoder.encode(pwd,"UTF-8")}"

    //println("we"+CKAN_URL.split(":")(1))

    val host = CKAN_URL.split(":")(1).replaceAll("""//""", "")

    //println("HOST-->"+host)

    val url = wsClient.url(CKAN_URL + "/ldap_login_handler")
      .withHeaders("host" -> host,
        "User-Agent" ->"""Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:55.0) Gecko/20100101 Firefox/55.0""",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" -> "en-US,en;q=0.5",
        "Accept-Encoding" -> "gzip, deflate",
        "Referer" -> (CKAN_URL + "/user/login"),
        "Content-Type" -> "application/x-www-form-urlencoded",
        "Content-Length" -> login.length.toString,
        "Connection" -> "keep-alive",
        "Upgrade-Insecure-Requests" -> "1"
      ).withFollowRedirects(false)

    //println(">>>>"+url.headers)

    val wsResponse = url.post(login)

    println("login ckan2")

    wsResponse.map(getCookies(_).head)

  }

  private def loginJupyter(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val login = s"username=$userName&password=${URLEncoder.encode(pwd,"UTF-8")}"

    val host = JUPYTER_URL.split(":")(1).replaceAll("""//""", "")

    val url = wsClient.url(JUPYTER_URL + "/hub/login?next=")
      .withHeaders("host" -> host,
        "Accept" -> "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Referer" -> (JUPYTER_URL + "/hub/login"),
        "Content-Type" -> "application/x-www-form-urlencoded",
        "Content-Length" -> login.length.toString,
        "Connection" -> "keep-alive",
        "Upgrade-Insecure-Requests" -> "1"
      ).withFollowRedirects(false)

    val wsResponse = url.post(login)

    println("login jupyter")

    wsResponse.map(getCookies(_).head)

  }

  private def loginIPA(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val login = s"user=$userName&password=${URLEncoder.encode(pwd,"UTF-8")}"

    val wsResponse = wsClient.url(IPA_LOGIN_ULR).withHeaders("Content-Type" -> "application/x-www-form-urlencoded",
      "Accept" -> "text/plain",
      "referer" -> IPA_APP_ULR
    ).post(login)

    println("login IPA")

    wsResponse.map(getCookies(_).head)

  }


  private def loginSuperset(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val data = Json.obj(
      "username" -> userName,
      "password" -> pwd
    )
    //val sessionFuture: Future[String] = ws.url(local + "/superset/session").get().map(_.body)
    val wsResponse: Future[WSResponse] = wsClient.url(SUPERSET_URL + "/login/").withHeaders("Content-Type" -> "application/json").post(data)

    println("login superset")

    wsResponse.map(getCookies(_).head)

  }


  private def loginMetabase(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val data = Json.obj(
      "username" -> userName,
      "password" -> pwd
    )


    println("login metabase")

    val responseWs: Future[WSResponse] = wsClient.url(METABASE_URL + "/api/session").withHeaders("Content-Type" -> "application/json").post(data)
    responseWs.map { response =>

      val cookie = (response.json \ "id").as[String]
      println("COOKIE(metabase): " + cookie)
      Cookie("metabase.SESSION_ID",cookie)
    }

  }


  private def loginGrafanaFE(userName: String, pwd: String, wsClient: WSClient): Future[Seq[Cookie]] = {

    val data = Json.obj(
      "user" -> userName,
      "password" -> pwd
    )

    val wsResponse: Future[WSResponse] = wsClient.url(GRAFANA_URL + "/login/").withHeaders("Content-Type" -> "application/json").post(data)

    println("login grafana")

    wsResponse.map(getCookies)

  }

  private def loginGrafana(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    loginGrafanaFE(userName, pwd, wsClient).map(_.find(_.name=="grafana_sess").get )

  }



  private def getCookies(response:WSResponse):Seq[Cookie]={

    response.header("Set-Cookie").getOrElse(throw new Exception("Set-Cookie header not found"))

    //Cookie(name: String, value: String, maxAge: Option[Int] = None, path: String = "/", domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)
    println("cookies-->"+response.cookies)

    response.cookies.map{ wscookie =>

      Cookie( wscookie.name.get,
              wscookie.value.getOrElse(""),
              wscookie.maxAge.map(_.toInt),
              wscookie.path,
              Option(wscookie.domain),
              wscookie.secure)

    }

  }


}


object LoginClientLocal {

  val CKAN = "ckan"
  val FREE_IPA = "freeIPA"
  val SUPERSET = "superset"
  val METABASE = "metabase"
  val JUPYTER = "jupyter"
  val GRAFANA = "grafana"

}



