package it.gov.daf.securitymanager.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.securitymanager.service.utilities.{ConfigReader, WebServiceUtil}
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import security_manager.yaml.IpaUser
import security_manager.yaml.Error
import security_manager.yaml.Success

import scala.concurrent.Future

object ApiClientIPA {

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  private val CKAN_URL = ConfigReader.getCkanHost
  private val IPA_URL = ConfigReader.ipaUrl
  private val IPA_APP_ULR = IPA_URL+"/ipa"
  private val IPA_SERVICES_URL = IPA_URL+"/ipa/session/json"
  private val IPA_LOGIN_ULR = IPA_URL+"/ipa/session/login_password"
  private val USER_PASSWORD_POST_DATA = s"user=${ConfigReader.ipaUser}&password=${ConfigReader.ipaUserPwd}"
  private val sslconfig = new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build

  private var sessionCookie:String=null




  private def login( wsClient:AhcWSClient ):Future[String] = {

    val wsResponse = wsClient.url(IPA_LOGIN_ULR).withHeaders(  "Content-Type"->"application/x-www-form-urlencoded",
                                              "Accept"->"text/plain",
                                              "referer"->IPA_APP_ULR
    ).post(USER_PASSWORD_POST_DATA)

    println("login")

    wsResponse map { response =>

      val setCookie=response.header("Set-Cookie").getOrElse( throw new Exception("Set-Cookie header not found") )
      println("SET COOKIE: "+setCookie)
      val cookie = setCookie.split(";")(0)
      println("COOKIE: "+cookie)
      cookie

    }

  }

  private def loginCkan(userName:String, pwd:String ):Future[String] = {

    val wsClient = AhcWSClient()

    val login = s"login=$userName&password=$pwd"//&remember=63072000

    val url = wsClient.url(CKAN_URL+"/ldap_login_handler")
      .withHeaders(   //"Host"->"localhost:5000",
                      "User-Agent"->"""Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:55.0) Gecko/20100101 Firefox/55.0""",
                      "Accept"->"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                      "Accept-Language"-> "en-US,en;q=0.5",
                      "Accept-Encoding"-> "gzip, deflate",
                      "Referer"-> (CKAN_URL+"/user/login"),
                      "Content-Type"->"application/x-www-form-urlencoded",
                      "Content-Length"-> login.length.toString,
                      "Connection"-> "keep-alive",
                      "Upgrade-Insecure-Requests"-> "1"
    )

    //url.followRedirects = Option(true)
    //println(">>>>"+url.headers)
    val wsResponse = url.post(login)

    println("login ckan")


    wsResponse.map({ response =>

      if(response.status==200)
        "ok"
      else
        throw new Exception("response status not valid")
      /*
      println("-->"+response.status)
      println("--->"+response.body)
      println("---->"+response.allHeaders)
      */
      /*
      val setCookie=response.header("Set-cookie").getOrElse( throw new Exception("Set-Cookie header not found") )
      println("(CKAN) SET COOKIE: "+setCookie)
      val cookie = setCookie.split(";")(0)
      println("(CKAN) COOKIE: "+cookie)
      cookie
      */

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


  private def callIpaService( wsClient:AhcWSClient,jsIn:JsValue, fx:(AhcWSClient,JsValue)  => Future[WSResponse]):Future[WSResponse] = {


    if( sessionCookie == null )

      login(wsClient).flatMap { cookie =>

        sessionCookie = cookie

        fx(wsClient, jsIn ).map({ response =>
          println("RESPONSE:"+response.json)
          response
        }).andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }

      }

    else

      fx(wsClient, jsIn).map{ response =>
        if(response.status == 200)
          println("RESPONSE:"+response.json)
        response
      }


  }



  private def call(jsIn:JsValue, fx:(AhcWSClient,JsValue)  => Future[WSResponse]) : Future[JsValue] = {

    val wsClient = AhcWSClient(sslconfig)

    callIpaService(wsClient,jsIn,fx) flatMap {response1 =>

      if(response1.status == 401){
        println("Unauthorized!!")
        sessionCookie=null
        callIpaService(wsClient,jsIn,fx).map({response2 => response2.json})
          .andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }
      }else
         Future{ response1.json }

    }

  }


  def createUser(user: IpaUser):Future[Either[Error,Success]]= {


    val jsonUser: JsValue = Json.parse(
      s"""{
                                       "method":"user_add",
                                       "params":[
                                          [
                                             "${user.uid}"
                                          ],
                                          {
                                             "cn":"${user.givenname + " " + user.sn}",
                                             "displayname":"${user.givenname + " " + user.sn}",
                                             "givenname":"${user.givenname}",
                                             "sn":"${user.sn}",
                                             "mail":"${user.mail}",
                                             "userpassword":"${user.userpassword.get}",

                                             "no_members":false,
                                             "noprivate":false,
                                             "random":false,
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println(jsonUser.toString())

    call(jsonUser, callIpaUrl).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null") {
        loginCkan(user.uid, user.userpassword.get).map { _ =>
          Right(Success(Some("User created"), Some("ok")))
        }
      } else Future { Left( Error(None,Some(readIpaErrorMessage(json)),None) ) }

    }

  }


  def showUser(userId: String):Future[Either[Error,IpaUser]]={

    val jsonRequest:JsValue = Json.parse(s"""{
                                             "id": 0,
                                             "method": "user_show/1",
                                             "params": [
                                                 [
                                                     "$userId"
                                                 ],
                                                 {
                                                     "version": "2.213"
                                                 }
                                             ]
                                         }""")

    println(jsonRequest.toString())

    call(jsonRequest,callIpaUrl).map { json =>

      val result = ((json \ "result") \"result")//.getOrElse(JsString("null")).toString()

      if( result == "null" || result.isInstanceOf[JsUndefined] )

        Left( Error(None,Some(readIpaErrorMessage(json)),None) )

      else
        Right(
          IpaUser(
            (result \ "sn")(0).asOpt[String].getOrElse(""),
            (result \ "givenname")(0).asOpt[String].getOrElse(""),
            (result \ "mail")(0).asOpt[String].getOrElse(""),
            (result \ "uid")(0).asOpt[String].getOrElse(""),
            None
          )
        )

    }

  }


  private def callIpaUrl( cli:AhcWSClient, payload: JsValue ): Future[WSResponse] = {

    cli.url(IPA_SERVICES_URL).withHeaders(  "Content-Type"->"application/json",
      "Accept"->"application/json",
      "referer"->IPA_APP_ULR,
      "Cookie" -> sessionCookie
    ).post(payload)

  }

  private def readIpaErrorMessage( json:JsValue )={

    val error = (json \ "error").getOrElse(JsString("null")).toString()
    if( error != "null" )
      WebServiceUtil.cleanDquote( ((json \ "error") \"message").get.toString() )
    else
      "Unexpeted error"

  }



  /*
def createUser(jsonUser: JsValue): Future[JsValue] = {

  println("create user")

  val wsClient = AhcWSClient(sslconfig)

  login(wsClient).flatMap{cook=>

    sessionCookie=cook

    println("sessionCookieee:"+sessionCookie)

    val body:String = "{\"method\":\"user_add\",\"params\":[[\"foobar5\"],{ \"cn\": \"foo bar\", \"displayname\": \"foo bar\", \"gecos\": \"foo bar\", \"givenname\": \"foo\", \"initials\": \"fb\", \"krbprincipalname\": \"foobar@EXAMPLE.TEST\",  \"no_members\": false, \"noprivate\": false, \"random\": false, \"raw\": false, \"sn\": \"bar\" }],\"id\":0}"

    wsClient.url(IPA_SERVICES_URL).withHeaders(   "Content-Type"->"application/json",
      "Accept"->"application/json",
      "referer"->IPA_APP_ULR,
      "Cookie" -> sessionCookie
    ).post(body).map({ response =>

      println("responsee "+response)

      //response.json

      if(response.status == 401){
        sessionCookie=null
        JsString("Unauthorized")
      }else{
        println("RESSSP: "+response.json)
        response.json
      }


    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

}*/


}