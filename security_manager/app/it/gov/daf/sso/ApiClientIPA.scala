package it.gov.daf.sso

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.securitymanager.service.utilities.{ConfigReader, WebServiceUtil}
import it.gov.daf.sso.common.{LoginInfo, SecuredInvocationManager}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import security_manager.yaml.{Error, IpaUser, Success}

import scala.concurrent.Future


object ApiClientIPA {

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  private val loginInfo = new LoginInfo(ConfigReader.ipaUser, ConfigReader.ipaUserPwd, LoginClientLocal.FREE_IPA)
  private val loginClient = LoginClientLocal.instance()
  private val secInvokeManager = SecuredInvocationManager.instance(loginClient)


  //private val CKAN_URL = ConfigReader.getCkanHost
  //private val IPA_URL = ConfigReader.ipaUrl
  //private val IPA_APP_ULR = IPA_URL+"/ipa"
  //private val IPA_SERVICES_URL = IPA_URL+"/ipa/session/json"
  //private val IPA_LOGIN_ULR = IPA_URL+"/ipa/session/login_password"
  //private val USER_PASSWORD_POST_DATA = s"user=${ConfigReader.ipaUser}&password=${ConfigReader.ipaUserPwd}"
  //private val sslconfig = new DefaultAsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build

  //private var sessionCookie:String=null



/*
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
*/

/*

  private def callIpaService( wsClient:AhcWSClient,jsIn:JsValue, serviceCall:(AhcWSClient,JsValue)  => Future[WSResponse]):Future[WSResponse] = {


    if( sessionCookie == null )

      login(wsClient).flatMap { cookie =>

        sessionCookie = cookie

        serviceCall(wsClient, jsIn ).map({ response =>
          println("RESPONSE:"+response.json)
          response
        }).andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }

      }

    else

      serviceCall(wsClient, jsIn).map{ response =>
        if(response.status == 200)
          println("RESPONSE:"+response.json)
        response
      }


  }



  private def manageServiceCall(payload:JsValue, serviceCall:(AhcWSClient,JsValue)  => Future[WSResponse]) : Future[JsValue] = {

    val wsClient = AhcWSClient(sslconfig)

    callIpaService(wsClient,payload,serviceCall) flatMap {response1 =>

      if(response1.status == 401){
        println("Unauthorized!!")
        sessionCookie=null
        callIpaService(wsClient,payload,serviceCall).map({response2 => response2.json})
          .andThen { case _ => wsClient.close() }
          .andThen { case _ => system.terminate() }
      }else
         Future{ response1.json }

    }

  }*/


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

    val serviceInvoke : (String,AhcWSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null") {
        loginCkan(user.uid, user.userpassword.get).map { _ =>
          Right(Success(Some("User created"), Some("ok")))
        }
      } else Future { Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) }

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

    val serviceInvoke : (String,AhcWSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>

      val result = ((json \ "result") \"result")//.getOrElse(JsString("null")).toString()

      if( result == "null" || result.isInstanceOf[JsUndefined] )

        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )

      else
        Right(
          IpaUser(
            (result \ "sn") (0).asOpt[String].getOrElse(""),
            (result \ "givenname") (0).asOpt[String].getOrElse(""),
            (result \ "mail") (0).asOpt[String].getOrElse(""),
            (result \ "uid") (0).asOpt[String].getOrElse(""),
            None
          )
        )

    }

  }


  private def callIpaUrl( payload: JsValue, sessionCookie:String, cli:AhcWSClient ): Future[WSResponse] = {

    cli.url(ConfigReader.ipaUrl+"/ipa/session/json").withHeaders( "Content-Type"->"application/json",
      "Accept"->"application/json",
      "referer"->(ConfigReader.ipaUrl+"/ipa"),
      "Cookie" -> sessionCookie
    ).post(payload)

  }

  private def loginCkan(userName:String, pwd:String ):Future[String] = {

    val wsClient = AhcWSClient()

    println("login ckan")

    val loginInfo = new LoginInfo(userName,pwd,LoginClientLocal.CKAN)
    val wsResponse = loginClient.login(loginInfo,wsClient)

    wsResponse.map({ response =>

      if( response != null  )
        "ok"
      else
        throw new Exception("Failed to login to ckan")


    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  private def readIpaErrorMessage( json:JsValue )={

    val error = (json \ "error").getOrElse(JsString("null")).toString()
    if( error != "null" )
      WebServiceUtil.cleanDquote( ((json \ "error") \"message").get.toString() )
    else
      "Unexpeted error"

  }


}