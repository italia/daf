package it.gov.daf.sso

import com.google.inject.{Inject, Provides, Singleton}
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.common.authentication.Role
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, Group, IpaUser, Success, UserList}

import scala.concurrent.Future

@Singleton
class ApiClientIPA @Inject()(secInvokeManager:SecuredInvocationManager){

  import scala.concurrent.ExecutionContext.Implicits._

  private val loginInfo = new LoginInfo(ConfigReader.ipaUser, ConfigReader.ipaUserPwd, LoginClientLocal.FREE_IPA)


  def createUser(user: IpaUser):Future[Either[Error,Success]]= {

    val role = user.role.getOrElse(Role.Viewer.toString)

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
                                             "userclass":"$role",
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println(jsonUser.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")// {
      //  loginCkan(user.uid, user.userpassword.get).map { _ =>
        Future { Right(Success(Some("User created"), Some("ok"))) }
       // }
    //  }
      else
        Future { Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) }

    }

  }


  def deleteUser(userUid: String):Future[Either[Error,Success]]= {


    val jsonDelete: JsValue = Json.parse(
                                s"""{
                                       "method":"user_del",
                                       "params":[
                                          [
                                             "${userUid}"
                                          ],
                                          {
                                             "continue":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("deleteUser: "+jsonDelete.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonDelete,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
        Future.successful( Right(Success(Some("User deleted"), Some("ok"))) )
      else
        Future.successful( Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) )

    }

  }


  def createGroup(group: String):Future[Either[Error,Success]]= {

    val jsonGroup: JsValue = Json.parse(
                                s"""{
                                       "method":"group_add",
                                       "params":[
                                          [
                                             "${group}"
                                          ],
                                          {
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("createGroup: "+ jsonGroup.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonGroup,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>

      val result = ((json \ "result") \"result")

      if( result == "null" || result.isInstanceOf[JsUndefined] )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right( Success(Some("Group created"), Some("ok")) )

    }

  }

  def deleteGroup(group: String):Future[Either[Error,Success]]= {


    val jsonDelete: JsValue = Json.parse(
                                s"""{
                                       "method":"group_del",
                                       "params":[
                                          [
                                             "${group}"
                                          ],
                                          {
                                             "continue":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("deleteGroup: "+jsonDelete.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonDelete,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
        Future.successful( Right(Success(Some("Group deleted"), Some("ok"))) )
      else
        Future.successful(Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) )

    }

  }

  def addUsersToGroup(group: String, userList: UserList):Future[Either[Error,Success]]= {

    val jArrayStr = userList.users.get.mkString("\"","\",\"","\"")

    val jsonAdd: JsValue = Json.parse(
                                s"""{
                                       "method":"group_add_member",
                                       "params":[
                                          [
                                             "$group"
                                          ],
                                          {
                                             "user":[$jArrayStr],
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("addUsersToGroup: "+ jsonAdd.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonAdd,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>
      val result = ((json \ "result") \"result")

      if( result == "null" || result.isInstanceOf[JsUndefined] )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right(Success(Some("Users added"), Some("ok")))
    }

  }

  def removeUsersFromGroup(group: String, userList: UserList):Future[Either[Error,Success]]= {

    val jArrayStr = userList.users.get.mkString("\"","\",\"","\"")

    val jsonAdd: JsValue = Json.parse(
      s"""{
                                       "method":"group_remove_member",
                                       "params":[
                                          [
                                             "$group"
                                          ],
                                          {
                                             "user":[$jArrayStr],
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("removeUsersToGroup: "+ jsonAdd.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonAdd,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>
      val result = ((json \ "result") \"result")

      if( result == "null" || result.isInstanceOf[JsUndefined] )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right(Success(Some("Users removed"), Some("ok")))
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
                                                     "all": "true",
                                                     "version": "2.213"
                                                 }
                                             ]
                                         }""")

    println("showUser request: "+jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)
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
            (result \ "userclass") (0).asOpt[String],
            None,
            (result \ "memberof_group").asOpt[Seq[String]]
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
                                                    "all": "true",
                                                    "version": "2.213"
                                                 }
                                             ]
                                         }""")

    println("findUserByMail request: "+ jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)
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
            (result \ "userclass") (0).asOpt[String],
            None,
            (result \ "memberof_group").asOpt[Seq[String]]
          )
        )

    }

  }

  private def callIpaUrl( payload: JsValue, sessionCookie:String, cli:WSClient ): Future[WSResponse] = {

    cli.url(ConfigReader.ipaUrl+"/ipa/session/json").withHeaders( "Content-Type"->"application/json",
      "Accept"->"application/json",
      "referer"->(ConfigReader.ipaUrl+"/ipa"),
      "Cookie" -> sessionCookie
    ).post(payload)

  }

  /*
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

  }*/

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