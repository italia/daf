package it.gov.daf.catalogmanager.tempo

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import catalog_manager.yaml.{Error, IpaUser, Success}
import it.gov.daf.catalogmanager.utilities.WebServiceUtil
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

object ApiClientIPA {

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  private val IPA_URL = "https://ipa.example.test"
  private val IPA_APP_ULR = IPA_URL+"/ipa"
  private val IPA_SERVICES_URL = IPA_URL+"/ipa/session/json"
  private val IPA_LOGIN_ULR = IPA_URL+"/ipa/session/login_password"
  private val USER_PASSWORD_POST_DATA = "user=admin&password=adminpassword"
  private val sslconfig = new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build

  private var sessionCookie:String=null


  private def login( wsClient:AhcWSClient ):Future[String] = {

    val wsResponse = wsClient.url(IPA_LOGIN_ULR).withHeaders(  "Content-Type"->"application/x-www-form-urlencoded",
                                              "Accept"->"text/plain",
                                              "referer"->IPA_APP_ULR
    ).post(USER_PASSWORD_POST_DATA)

    println("login")
    println("-->"+wsResponse)

    wsResponse map { response =>

      val setCookie=response.header("Set-Cookie").getOrElse( throw new Exception("haa") )
      println("SET COOKIE: "+setCookie)
      val cookie = setCookie.split(";")(0)
      println("COOKIE: "+cookie)
      cookie

    }

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

      fx( wsClient, jsIn ).map{ response =>
        if(response.status == 200)
          println("RESPONSE:"+response.json)
        response
      }


  }



  def call(jsIn:JsValue, fx:(AhcWSClient,JsValue)  => Future[WSResponse]) : Future[JsValue] = {

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


  def createUser(user: IpaUser):Future[Either[Error,Success]]={


    val jsonUser:JsValue = Json.parse(s"""{
                                       "method":"user_add",
                                       "params":[
                                          [
                                             "${user.uid.get}"
                                          ],
                                          {
                                             "cn":"${user.givenname.get+" "+user.sn.get}",
                                             "displayname":"${user.givenname.get+" "+ user.sn.get}",
                                             "givenname":"${user.givenname.get}",
                                             "sn":"${user.sn.get}",
                                             "mail":"${user.mail.get}",
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

    call(jsonUser,callIpaUrl).map { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if( result!= "null" )
        Right( Success(Some("User created"), Some("ok")) )

      else
        Left( Error(None,Some(readIpaErrorMessage(json)),None) )

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




      if( result!= "null" )

        Right(
          IpaUser(
            (result \ "sn")(0).asOpt[String],
            (result \ "givenname").asOpt[String],
            (result \ "mail")(0).asOpt[String],
            None,
            (result \ "uid")(0).asOpt[String]
          )
        )

      else
        Left( Error(None,Some(readIpaErrorMessage(json)),None) )

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