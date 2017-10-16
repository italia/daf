package it.gov.daf.sso

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.securitymanager.service.utilities.{ConfigReader, WebServiceUtil}
import it.gov.daf.sso.common.{LoginInfo, SecuredInvocationManager}
import org.apache.commons.lang3.StringEscapeUtils
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
                                             "userpassword":"${StringEscapeUtils.escapeJson(user.userpassword.get)}",

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

  def findUserByMail(mail: String):Future[Either[Error,IpaUser]]={

    val jsonRequest:JsValue = Json.parse(s"""{
                                             "id": 0,
                                             "method": "user_find",
                                             "params": [
                                                 [""],
                                                 {
                                                    "mail": "$mail",
                                                    "version": "2.213"
                                                 }
                                             ]
                                         }""")

    //println(jsonRequest.toString())

    val serviceInvoke : (String,AhcWSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val result = ((json \ "result") \"result")(0)//.getOrElse(JsString("null")).toString()

      if(count==0)
        Left( Error(Option(0),Some("No user found"),None) )

      else if( result == "null" || result.isInstanceOf[JsUndefined]  )
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

  /*
  private def bindDefaultOrg(userName:String):Future[String] = {

    val wsClient = AhcWSClient()

    println("bind default organization")

    wsClient.url()

    wsResponse.map({ response =>

      if( response != null  )
        "ok"
      else
        throw new Exception("Failed to login to ckan")


    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }*/

  private def readIpaErrorMessage( json:JsValue )={

    val error = (json \ "error").getOrElse(JsString("null")).toString()
    if( error != "null" )
      WebServiceUtil.cleanDquote( ((json \ "error") \"message").get.toString() )
    else
      "Unexpeted error"

  }


}