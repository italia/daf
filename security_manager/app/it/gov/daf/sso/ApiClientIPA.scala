package it.gov.daf.sso

import java.net.URLEncoder

import com.google.inject.{Inject, Provides, Singleton}
import it.gov.daf.common.authentication.Role
import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo, SecuredInvocationManager}
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.service.IntegrationService
import it.gov.daf.securitymanager.service.utilities.{AppConstants, ConfigReader}
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, IpaGroup, IpaUser, Success}

import scala.concurrent.Future

@Singleton
class ApiClientIPA @Inject()(secInvokeManager:SecuredInvocationManager,loginClientLocal: LoginClientLocal,wsClient: WSClient, cacheWrapper: CacheWrapper){

  import scala.concurrent.ExecutionContext.Implicits._

  private val loginInfo = new LoginInfo(ConfigReader.ipaUser, ConfigReader.ipaUserPwd, LoginClientLocal.FREE_IPA)


  def createUser(user: IpaUser, isPredefinedOrgUser:Boolean):Future[Either[Error,Success]]= {

    //"userpassword":"${StringEscapeUtils.escapeJson(user.userpassword.get)}",
    val titleAttributeValue = if(isPredefinedOrgUser) AppConstants.PredefinedOrgUserTitle else ""

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

                                             "title": "$titleAttributeValue",
                                             "no_members":false,
                                             "noprivate":false,
                                             "random":true,
                                             "raw":false,

                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("createUser: "+jsonUser.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()


      if(result != "null") {
        loginCkan(user.uid, user.userpassword.get).map { _ =>
          val randomPwd = (((json \ "result")\"result")\"randompassword").asOpt[String]
          Right(Success(Some("User created"), randomPwd))
        }
      }
      else
        Future { Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) }

    }

  }


  def updateUser(userUid: String, givenname:String, sn:String):Future[Either[Error,Success]]= {

    val jsonUser: JsValue = Json.parse(
      s"""{
                                       "method":"user_mod",
                                       "params":[
                                          [
                                             "$userUid"
                                          ],
                                          {
                                             "cn":"${givenname + " " + sn}",
                                             "displayname":"${givenname + " " + sn}",
                                             "givenname":"${givenname}",
                                             "sn":"$sn",

                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println(jsonUser.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
        Future{ Right(Success(Some("User modified"), Some("ok"))) }
      else if( ((json \ "error")\"code").as[Long] == 4202 )
        Future{ Right(Success(Some("User not modified"), Some("ok"))) }
      else
        Future { Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) }

    }

  }


  def passwd(userUid: String, oldPwd:String, newPwd:String):Future[Either[Error,Success]]= {

    //val oldPwd = cacheWrapper.getPwd(userUid).get "principal": "password": "current_password":

    val jsonUser: JsValue = Json.parse(
      s"""{
                                       "method":"passwd",
                                       "params":[
                                          [
                                             "$userUid",
                                             "$newPwd",
                                             "$oldPwd"
                                          ],
                                          {
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("passwd: "+jsonUser.toString())

    //val currentUserLoginInfo = new LoginInfo(userUid, oldPwd, LoginClientLocal.FREE_IPA)
    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)

    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).flatMap { json =>

      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
        Future{ Right(Success(Some("Password changed"), Some("ok"))) }
      //else if( ((json \ "error")\"code").as[Long] == 4202 )
        //Future{ Right(Success(Some("User not modified"), Some("ok"))) }
      else
        Future { Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) }

    }

  }

  private val IPA_URL = ConfigReader.ipaUrl
  private val IPA_APP_ULR = IPA_URL + "/ipa"
  private val IPA_CHPWD_ULR = IPA_URL + "/ipa/session/change_password"

  def changePassword(userUid: String, oldPwd:String, newPwd:String):Future[Either[Error,Success]]= {

    val login = s"user=$userUid&old_password=${URLEncoder.encode(oldPwd,"UTF-8")}&new_password=${URLEncoder.encode(newPwd,"UTF-8")}"

    val wsResponse = wsClient.url(IPA_CHPWD_ULR).withHeaders("Content-Type" -> "application/x-www-form-urlencoded",
      "Accept" -> "text/plain",
      "referer" -> IPA_APP_ULR
    ).post(login)

    println("login IPA: "+login)

    wsResponse.map( _.status match{
      case 200 =>  Right(Success(Some("Password changed"), Some("ok")))
      case _ => Left( Error(Option(0),Some("Error in changePassword"),None) )
    } )

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
                                             "description":"${AppConstants.OrganizationIpaGroupDescription}",
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

  def showGroup(group: String):Future[Either[Error,IpaGroup]]= {

    val jsonGroup: JsValue = Json.parse(
      s"""{
                                       "method":"group_show",
                                       "params":[
                                          [
                                             "${group}"
                                          ],
                                          {
                                             "all":true,
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    println("showGroup: "+ jsonGroup.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonGroup,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>

      val result = ((json \ "result") \"result")//.getOrElse(JsString("null")).toString()

      if( result == "null" || result.isInstanceOf[JsUndefined] )

        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right(
          IpaGroup(
            (result \ "dn").asOpt[String].getOrElse(""),
            (result \ "gidnumber") (0).asOpt[String],
            (result \ "member_user").asOpt[Seq[String]]
          )
        )
    }

  }

  def isEmptyGroup(groupCn:String):Future[Either[Error,Success]] ={

    showGroup(groupCn) map{
      case Right(r) =>  if(r.member_user.nonEmpty && r.member_user.get.filter(p => !p.equals(IntegrationService.toUserName(groupCn))).nonEmpty)
                          Left(Error(Option(1),Some("This group contains users"),None))
                        else
                          Right(Success(Some("Empty group"), Some("ok")))

      case Left(l) => Left(l)
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

  def addUsersToGroup(group: String, userList: Seq[String]):Future[Either[Error,Success]]= {

    val jArrayStr = userList.mkString("\"","\",\"","\"")

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

  def removeUsersFromGroup(group: String, userList: Seq[String]):Future[Either[Error,Success]]= {

    val jArrayStr = userList.mkString("\"","\",\"","\"")

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

  def findUserByUid(userId: String):Future[Either[Error,IpaUser]]={

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

    println("findUserByUid request: "+jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val result = ((json \ "result") \"result")

      if(count==0)
        Left( Error(Option(1),Some("No user found"),None) )

      if( result == "null" || result.isInstanceOf[JsUndefined] )

        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )

      else
        Right(
          IpaUser(
            sn = (result \ "sn") (0).asOpt[String].getOrElse(""),
            givenname = (result \ "givenname") (0).asOpt[String].getOrElse(""),
            mail = (result \ "mail") (0).asOpt[String].getOrElse(""),
            uid = (result \ "uid") (0).asOpt[String].getOrElse(""),
            role = ApiClientIPA.extractRole( (result \ "memberof_group").asOpt[Seq[String]] ),
            title = (result \ "title") (0).asOpt[String],
            userpassword = None,
            organizations = ApiClientIPA.extractOrgs( (result \ "memberof_group").asOpt[Seq[String]] )
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
        Left( Error(Option(1),Some("No user found"),None) )

      else if( result == "null" || result.isInstanceOf[JsUndefined]  )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )

      else
        Right(
          IpaUser(
            sn = (result \ "sn") (0).asOpt[String].getOrElse(""),
            givenname = (result \ "givenname") (0).asOpt[String].getOrElse(""),
            mail = (result \ "mail") (0).asOpt[String].getOrElse(""),
            uid = (result \ "uid") (0).asOpt[String].getOrElse(""),
            role = ApiClientIPA.extractRole( (result \ "memberof_group").asOpt[Seq[String]] ),
            title = (result \ "title") (0).asOpt[String],
            userpassword = None,
            organizations = ApiClientIPA.extractOrgs( (result \ "memberof_group").asOpt[Seq[String]] )
          )
        )

    }

  }

  def organizationList():Future[Either[Error,Seq[String]]]={

    val jsonRequest:JsValue = Json.parse(s"""{
                                             "id": 0,
                                             "method": "group_find",
                                             "params": [
                                                 [""],
                                                 {
                                                    "description": "${AppConstants.OrganizationIpaGroupDescription}",
                                                    "all": "false",
                                                    "raw": "true",
                                                    "version": "2.213"
                                                 }
                                             ]
                                         }""")

    println("findUserByMail request: "+ jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)
    secInvokeManager.manageServiceCall(loginInfo,serviceInvoke).map { json =>

      //Right(Success(Some("ok"), Some("ok")))

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val result = ((json \ "result") \"result")

      if(count==0)
        Left( Error(Option(1),Some("No organization founded"),None) )

      else if( result == "null" || result.isInstanceOf[JsUndefined]  )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )

      else
        Right(
          result.asOpt[Seq[JsObject]].get map{ el => ((el \"cn")(0)).asOpt[String].get }
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


  private def loginCkan(userName:String, pwd:String):Future[String] = {

    println("login ckan")

    val loginInfo = new LoginInfo(userName,pwd,LoginClientLocal.CKAN)
    val wsResponse = loginClientLocal.login(loginInfo,wsClient)

    wsResponse.map(_=>"ok")
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



object ApiClientIPA {

  val whiteList = Seq( Role.Admin.toString(), Role.Editor.toString(), Role.Viewer.toString() )
  val blackList = Seq( Role.Admin.toString(), Role.Editor.toString(), Role.Viewer.toString(), "ipausers" )

  def extractRole( in:Option[Seq[String]] ): Option[String] = {

    if(in.isEmpty)
      None
    else{
      val out = in.get.filter(elem => whiteList.contains(elem))
      if( out.isEmpty )
        None
      else
        Option(out(0))
    }

  }

  def extractOrgs( in:Option[Seq[String]]): Option[Seq[String]] = {

    if(in.isEmpty)
      in
    else
      //Option( in.get.filter(elem => show) )
      Option( in.get.filter(elem => !blackList.contains(elem)) )
  }

  def isValidRole(role:String): Boolean = {
    whiteList.contains(role)
  }


}