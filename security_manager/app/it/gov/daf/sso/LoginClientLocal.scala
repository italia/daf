package it.gov.daf.sso

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Singleton
import it.gov.daf.common.sso.common.{LoginClient, LoginInfo}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Cookie
import play.api.libs.concurrent.Execution.Implicits._
import play.mvc.Http.Cookies

import scala.concurrent.Future
import scala.concurrent.stm.Txn.ExplicitRetryCause

@Singleton
class LoginClientLocal() extends LoginClient {

  private val logger = Logger(this.getClass.getName)
/*
  private val CKAN = "ckan"
  private val FREE_IPA = "freeIPA"
  private val SUPERSET = "superset"
  private val METABASE = "metabase"
  private val JUPYTER = "jupyter"
  private val GRAFANA = "grafana"
*/
  //private val CKAN_URL = ConfigReader.ckanHost
  private val CKAN_GEO_URL = ConfigReader.ckanGeoHost
  private val IPA_URL = ConfigReader.ipaUrl
  private val SUPERSET_URL = ConfigReader.supersetUrl
  private val SUPERSET_OPEN_URL = ConfigReader.supersetOpenUrl
  private val METABASE_URL = ConfigReader.metabaseUrl
  private val JUPYTER_URL = ConfigReader.jupyterUrl
  private val GRAFANA_URL = ConfigReader.grafanaUrl
  private val IPA_APP_ULR = IPA_URL + "/ipa"
  private val IPA_LOGIN_ULR = IPA_URL + "/ipa/session/login_password"

  def login(loginInfo: LoginInfo, wsClient: WSClient): Future[Cookie] = {

    loginInfo.appName match {
      //case LoginClientLocal.CKAN => loginCkan(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.CKAN_GEO => loginCkanGeo(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.FREE_IPA => loginIPA(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.SUPERSET => loginSuperset(loginInfo.user, loginInfo.password,wsClient, false)
      case LoginClientLocal.SUPERSET_OPEN => loginSuperset(loginInfo.user, loginInfo.password, wsClient, true)
      case LoginClientLocal.METABASE => loginMetabase(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.JUPYTER => loginJupyter(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.GRAFANA => loginGrafana(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.HADOOP => loginWebHDFS(loginInfo.user, loginInfo.password)
      case _ => throw new Exception("Unexpeted exception: application name not found")
    }

  }

  def loginFE(loginInfo: LoginInfo, wsClient: WSClient): Future[Seq[Cookie]] = {

    loginInfo.appName match {
      //case LoginClientLocal.CKAN => loginCkan(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.CKAN_GEO => loginCkanGeo(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.FREE_IPA => loginIPA(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.SUPERSET => loginSuperset(loginInfo.user, loginInfo.password, wsClient,false).map(Seq(_))
      case LoginClientLocal.SUPERSET_OPEN => loginSuperset(loginInfo.user, loginInfo.password, wsClient,true).map(Seq(_))
      case LoginClientLocal.METABASE => loginMetabase(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.JUPYTER => loginJupyter(loginInfo.user, loginInfo.password, wsClient).map(Seq(_))
      case LoginClientLocal.GRAFANA => loginGrafanaFE(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.HADOOP => loginWebHDFS(loginInfo.user, loginInfo.password).map(Seq(_))
      case _ => throw new Exception("Unexpeted exception: application name not found")
    }

  }


  def loginAdmin(appName: String, wsClient: WSClient): Future[Cookie] = {

    appName match {
      //case LoginClientLocal.CKAN => loginCkan( ConfigReader.ckanAdminUser, ConfigReader.ckanAdminPwd, wsClient)
      case LoginClientLocal.CKAN_GEO => loginCkanGeo(ConfigReader.ckanGeoAdminUser, ConfigReader.ckanGeoAdminPwd, wsClient)
      case LoginClientLocal.FREE_IPA => loginIPA( ConfigReader.ipaUser, ConfigReader.ipaUserPwd, wsClient)
      case LoginClientLocal.SUPERSET => loginSuperset( ConfigReader.suspersetAdminUser, ConfigReader.suspersetAdminPwd, wsClient,false)
      case LoginClientLocal.SUPERSET_OPEN => loginSuperset( ConfigReader.suspersetOpenDataUser, ConfigReader.suspersetOpenDataPwd, wsClient,true)
      case LoginClientLocal.METABASE => loginMetabase(ConfigReader.metabaseAdminUser, ConfigReader.metabaseAdminPwd, wsClient)
      case LoginClientLocal.JUPYTER => val msg="Jupyter dosen't have admins";logger.error(msg);throw new Exception(msg) //loginJupyter(loginInfo.user, loginInfo.password, wsClient)
      case LoginClientLocal.GRAFANA => loginGrafana(ConfigReader.grafanaAdminUser, ConfigReader.grafanaAdminPwd, wsClient)
      case LoginClientLocal.HADOOP => val msg="webHDFS dosen't have admins";logger.error(msg);throw new Exception(msg)
      case _ => throw new Exception("Unexpeted exception: application name not found")
    }

  }

  //private def loginCkan( userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = loginCkan(CKAN_URL,userName,pwd,wsClient)

  private def loginCkanGeo( userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = loginCkan(CKAN_GEO_URL,userName,pwd,wsClient)

  private def loginCkan( ckanUrl:String, userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val login = s"login=$userName&password=${URLEncoder.encode(pwd, "UTF-8")}"

    val host = ckanUrl.split(":")(1).replaceAll("""//""", "")

    println("XXX URL->" +ckanUrl + "/ldap_login_handler")
    println("XXX HOST->" +host)

    val url = wsClient.url(ckanUrl + "/ldap_login_handler")
      .withHeaders("host" -> host,
        "User-Agent" ->"""Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:55.0) Gecko/20100101 Firefox/55.0""",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" -> "en-US,en;q=0.5",
        "Accept-Encoding" -> "gzip, deflate",
        "Referer" -> (ckanUrl + "/user/login"),
        "Content-Type" -> "application/x-www-form-urlencoded",
        "Content-Length" -> login.length.toString,
        "Connection" -> "keep-alive",
        "Upgrade-Insecure-Requests" -> "1"
      ).withFollowRedirects(false)


    val wsResponse = url.post(login)

    logger.info("login ckan2")

    wsResponse.map(getCookies(_).head)

  }

  private def loginJupyter(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val login = s"username=$userName&password=${URLEncoder.encode(pwd, "UTF-8")}"

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

    logger.info("login jupyter")

    wsResponse.map(getCookies(_).head)

  }

  private def loginIPA(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val login = s"user=$userName&password=${URLEncoder.encode(pwd, "UTF-8")}"

    val wsResponse = wsClient.url(IPA_LOGIN_ULR).withHeaders("Content-Type" -> "application/x-www-form-urlencoded",
      "Accept" -> "text/plain",
      "referer" -> IPA_APP_ULR
    ).post(login)

    logger.info("login IPA")

    wsResponse.map(getCookies(_).head)

  }


  private def loginSuperset(userName: String, pwd: String, wsClient: WSClient, isOpenData:Boolean): Future[Cookie] = {

    val data = Json.obj(
      "username" -> userName,
      "password" -> pwd
    )

    val supersetUrl = if(isOpenData) SUPERSET_OPEN_URL else SUPERSET_URL
    val wsResponse: Future[WSResponse] = wsClient.url(supersetUrl + "/login/").withHeaders("Content-Type" -> "application/json").withFollowRedirects(false).post(data)

    logger.info("login superset")

    wsResponse.map(getCookies(_).head)

  }


  private def loginMetabase(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    val data = Json.obj(
      "username" -> userName,
      "password" -> pwd
    )


    logger.info("login metabase")

    val responseWs: Future[WSResponse] = wsClient.url(METABASE_URL + "/api/session").withHeaders("Content-Type" -> "application/json").withFollowRedirects(false).post(data)
    responseWs.map { response =>

      val cookie = (response.json \ "id").as[String]
      logger.debug("COOKIE(metabase): " + cookie)
      Cookie("metabase.SESSION_ID", cookie)
    }

  }


  private def loginWebHDFS(userName: String, pwd: String): Future[Cookie] = {

    logger.info("login WebHDFS")

    val out = WebHDFSLogin.loginF(userName,pwd) map{
      case Right(r) => r
      case Left(l) => throw new Exception(l)
    }

    out.map{ resp =>
      play.api.mvc.Cookies.fromSetCookieHeader( Some(resp) ).get("hadoop.auth") match{
        case Some(s) => s
        case None => throw new Exception(s"Hadoop cookie not found. WebHDFSLogin response:$resp")
      }
    }

  }

  private def loginGrafanaFE(userName: String, pwd: String, wsClient: WSClient): Future[Seq[Cookie]] = {

    val data = Json.obj(
      "user" -> userName,
      "password" -> pwd
    )

    val wsResponse: Future[WSResponse] = wsClient.url(GRAFANA_URL + "/login/").withHeaders("Content-Type" -> "application/json").withFollowRedirects(false).post(data)

    logger.info("login grafana")

    wsResponse.map(getCookies)

  }

  private def loginGrafana(userName: String, pwd: String, wsClient: WSClient): Future[Cookie] = {

    loginGrafanaFE(userName, pwd, wsClient).map(_.find(_.name == "grafana_sess").get)

  }


  private def getCookies(response: WSResponse): Seq[Cookie] = {

    logger.debug("RESPONSE IN GET COOKIES: " + response)

    val result = response.header("Set-Cookie")

    response.header("Set-Cookie") match{

      case Some(x) => logger.debug("cookies-->" + response.cookies)

                      response.cookies.map { wscookie =>

                        Cookie(wscookie.name.get,
                          wscookie.value.getOrElse(""),
                          wscookie.maxAge.map(_.toInt),
                          wscookie.path,
                          Option(wscookie.domain),
                          wscookie.secure)

                      }

      case None =>  logger.error("ERROR IN GET COOKIES: " + response.body)
                    throw new Exception("Set-Cookie header not found")
    }

  }
}


object LoginClientLocal {

  //val CKAN = "ckan"
  val CKAN_GEO = "ckan-geo"
  val FREE_IPA = "freeIPA"
  val SUPERSET = "superset"
  val SUPERSET_OPEN = "superset_open"
  val METABASE = "metabase"
  val JUPYTER = "jupyter"
  val GRAFANA = "grafana"
  val HADOOP = "hadoop"

}



