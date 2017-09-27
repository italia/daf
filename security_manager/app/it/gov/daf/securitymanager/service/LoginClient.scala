package it.gov.daf.securitymanager.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future


object LoginClient {


  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits._

  //private val CKAN_URL = "http://localhost:5000"

  val CKAN = "ckan"
  val FREE_IPA = "freeIPA"
  val SUPERSET = "superset"

  private val CKAN_URL = ConfigReader.getCkanHost
  private val IPA_URL = ConfigReader.ipaUrl
  private val SUPERSET_URL = ConfigReader.supersetUrl
  private val IPA_APP_ULR = IPA_URL+"/ipa"
  private val IPA_LOGIN_ULR = IPA_URL+"/ipa/session/login_password"


  def login(userName:String, pwd:String, wsClient: WSClient, appName:String):Future[String] = {

    appName match{
      case CKAN => loginCkan(userName,pwd,wsClient)
      case FREE_IPA => loginIPA(userName,pwd,wsClient)
      case SUPERSET => loginSuperset(userName,pwd,wsClient)
      case _ => throw new Exception("Unexpeted exception: application name not found")
    }

  }


  private def loginCkan( userName:String, pwd:String, wsClient: WSClient ):Future[String] = {

    val login = s"login=$userName&password=$pwd"

    //println("we"+CKAN_URL.split(":")(1))

    val host = CKAN_URL.split(":")(1).replaceAll("""//""","")

    //println("HOST-->"+host)

    val url = wsClient.url(CKAN_URL+"/ldap_login_handler")
      .withHeaders(   "host"->host,
                      "User-Agent"->"""Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:55.0) Gecko/20100101 Firefox/55.0""",
                      "Accept"->"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                      "Accept-Language"-> "en-US,en;q=0.5",
                      "Accept-Encoding"-> "gzip, deflate",
                      "Referer"-> (CKAN_URL+"/user/login"),
                      "Content-Type"->"application/x-www-form-urlencoded",
                      "Content-Length"-> login.length.toString,
                      "Connection"-> "keep-alive",
                      "Upgrade-Insecure-Requests"-> "1"
    ).withFollowRedirects(false)

    //url.followRedirects = Option(true)
    //println(">>>>"+url.headers)

    val wsResponse = url.post(login)

    println("login ckan2")

    wsResponse.map{ response =>


      println("STATUS-->"+response.status)
      //println("BODY-->"+response.body)
      println("HEADERS-->"+response.allHeaders)


      val setCookie=response.header("Set-cookie").getOrElse( throw new Exception("Set-Cookie header not found") )
      println("(CKAN) SET COOKIE: "+setCookie)
      val cookie = setCookie.split(";")(0)
      println("(CKAN) COOKIE: "+cookie)

      cookie

    }

  }


  private def loginIPA( userName:String, pwd:String, wsClient:WSClient ):Future[String] = {

    val login = s"user=$userName&password=$pwd"

    val wsResponse = wsClient.url(IPA_LOGIN_ULR).withHeaders(  "Content-Type"->"application/x-www-form-urlencoded",
      "Accept"->"text/plain",
      "referer"->IPA_APP_ULR
    ).post(login)

    println("login IPA")

    wsResponse map { response =>

      val setCookie=response.header("Set-Cookie").getOrElse( throw new Exception("Set-Cookie header not found") )
      println("SET COOKIE: "+setCookie)
      val cookie = setCookie.split(";")(0)
      println("COOKIE: "+cookie)
      cookie

    }

  }



  private def loginSuperset( userName:String, pwd:String, wsClient:WSClient ):Future[String] = {

    val data = Json.obj(
      "username" -> userName,
      "password" -> pwd
    )

    val wsResponse: Future[WSResponse] = wsClient.url(SUPERSET_URL + "/login/").post(data)

    println("login superset")

    wsResponse map { response =>

      val setCookie=response.header("Set-Cookie").getOrElse( throw new Exception("Set-Cookie header not found") )
      println("SET COOKIE: "+setCookie)
      val cookie = setCookie.split(";")(0)
      println("COOKIE: "+cookie)
      cookie

    }

  }


}
