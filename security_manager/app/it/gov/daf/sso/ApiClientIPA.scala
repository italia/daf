package it.gov.daf.sso

import java.net.URLEncoder

import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.authentication.Role
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.service.IntegrationService
import it.gov.daf.securitymanager.service.utilities.{AppConstants, ConfigReader}
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}
import security_manager.yaml.{Error, IpaGroup, IpaUser, Success}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger

@Singleton
class ApiClientIPA @Inject()(secInvokeManager:SecuredInvocationManager,loginClientLocal: LoginClientLocal,wsClient: WSClient){

  private val loginInfo = new LoginInfo(ConfigReader.ipaUser, ConfigReader.ipaUserPwd, LoginClientLocal.FREE_IPA)


  def testH:Future[Either[Error,Success]]={
    wsClient.url("https://master:50470/webhdfs/v1/app?op=GETFILESTATUS").withAuth("krbtgt/DAF.GOV.IT@DAF.GOV.IT","andreacherici@DAF.GOV.IT", WSAuthScheme.SPNEGO).get().map{ resp =>
      println("----->>>"+resp.body)
      Left( Error(Option(0),Some("wee"),None) )
    }
  }


  // only sysadmin and ipaAdmin
  def createUser(user: IpaUser, isPredefinedOrgUser:Boolean):Future[Either[Error,Success]]= {


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

    Logger.logger.debug("createUser: "+jsonUser.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if(result != "null") {

        val randomPwd = (((json \ "result")\"result")\"randompassword").asOpt[String]
        randomPwd match{
          case Some(rpwd) => loginCkan(user.uid, rpwd).map ( _ => Right(Success(Some("User created"), randomPwd)) )
          case None => Future{Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )}
        }

      }
      else
        Future { Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) ) }
    }


    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).flatMap {
      case Right(json) => handleJson(json)
      case Left(l) =>  Future { Left( Error(Option(0),Some(l),None) ) }
    }


  }

  // only sysadmin
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
                                             "givenname":"$givenname",
                                             "sn":"$sn",

                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    Logger.logger.debug("updateUser :"+jsonUser.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
        Right(Success(Some("User modified"), Some("ok")))
      else if( ((json \ "error")\"code").as[Long] == 4202 )
        Right(Success(Some("User not modified"), Some("ok")))
      else
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>   Left( Error(Option(0),Some(l),None) )
    }


  }

  def resetPwd(userUid: String):Future[Either[Error,Success]]= {

    val jsonUser: JsValue = Json.parse(
      s"""{
                                       "method":"user_mod",
                                       "params":[
                                          [
                                             "$userUid"
                                          ],
                                          {
                                             "random":true,

                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    Logger.logger.debug("resetPwd: "+jsonUser.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null" && ((json \ "error") \ "code").asOpt[Long].isEmpty){
        val randomPwd = (((json \ "result") \ "result") \ "randompassword").asOpt[String]
        Right(Success(Some("Password resetted"), randomPwd))
      }else
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) => Left( Error(Option(0),Some(l),None) )
    }

  }

  // NOT USED FOR NOW
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

    Logger.logger.debug("passwd: "+jsonUser.toString())

    //val currentUserLoginInfo = new LoginInfo(userUid, oldPwd, LoginClientLocal.FREE_IPA)
    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
        Right(Success(Some("Password changed"), Some("ok")))
      //else if( ((json \ "error")\"code").as[Long] == 4202 )
      //Future{ Right(Success(Some("User not modified"), Some("ok"))) }
      else
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }

  }

  private val IPA_URL = ConfigReader.ipaUrl
  private val IPA_APP_ULR = IPA_URL + "/ipa"
  private val IPA_CHPWD_ULR = IPA_URL + "/ipa/session/change_password"

  // only sysadmin and ipaAdmin
  def changePassword(userUid: String, oldPwd:String, newPwd:String):Future[Either[Error,Success]]= {

    val login = s"user=$userUid&old_password=${URLEncoder.encode(oldPwd,"UTF-8")}&new_password=${URLEncoder.encode(newPwd,"UTF-8")}"

    val wsResponse = wsClient.url(IPA_CHPWD_ULR).withHeaders("Content-Type" -> "application/x-www-form-urlencoded",
      "Accept" -> "text/plain",
      "referer" -> IPA_APP_ULR
    ).post(login)

    Logger.logger.debug("login IPA (changePassword): "+login)

    wsResponse.map{ response =>

      response.status match{
        case 200 =>   if( response.body.contains("change successful") )
                        Right(Success(Some("Password changed"), Some("ok")))
                      else{
                        println( "response"+ response.body )
                        Left( Error(Option(0),Some("Error in changePassword"),None) )
                      }


        case _ => println( "response"+ response.body ); Left( Error(Option(0),Some("Error in changePassword"),None) )
      }
    }

  }


  // only sysadmin
  def deleteUser(userUid: String):Future[Either[Error,Success]]= {


    val jsonDelete: JsValue = Json.parse(
                                s"""{
                                       "method":"user_del",
                                       "params":[
                                          [
                                             "$userUid"
                                          ],
                                          {
                                             "continue":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    Logger.logger.debug("deleteUser: "+jsonDelete.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonDelete,_,_)


    def handleJson(json:JsValue) = {
      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
         Right(Success(Some("User deleted"), Some("ok")))
      else
         Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
    }


    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )

    }

  }

  // only sysadmin
  def createGroup(group: String):Future[Either[Error,Success]]= {

    val jsonGroup: JsValue = Json.parse(
                                s"""{
                                       "method":"group_add",
                                       "params":[
                                          [
                                             "$group"
                                          ],
                                          {
                                             "description":"${AppConstants.OrganizationIpaGroupDescription}",
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    Logger.logger.debug("createGroup: "+ jsonGroup.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonGroup,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result") \"result"

      if( result.isInstanceOf[JsUndefined] )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right( Success(Some("Group created"), Some("ok")) )
    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }


  }

  // all users
  def showGroup(group: String):Future[Either[Error,IpaGroup]]= {

    val jsonGroup: JsValue = Json.parse(
      s"""{
                                       "method":"group_show",
                                       "params":[
                                          [
                                             "$group"
                                          ],
                                          {
                                             "all":true,
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    Logger.logger.debug("showGroup: "+ jsonGroup.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonGroup,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result") \"result"

      if( result.isInstanceOf[JsUndefined] )

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

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }


  }

  def isEmptyGroup(groupCn:String):Future[Either[Error,Success]] ={

    showGroup(groupCn) map{
      case Right(r) =>  if( r.member_user.nonEmpty && r.member_user.get.exists(p => !p.equals(IntegrationService.toUserName(groupCn))) )
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
                                             "$group"
                                          ],
                                          {
                                             "continue":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    Logger.logger.debug("deleteGroup: "+jsonDelete.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonDelete,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if (result != "null")
        Right(Success(Some("Group deleted"), Some("ok")))
      else
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )

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

    Logger.logger.debug("addUsersToGroup: "+ jsonAdd.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonAdd,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result") \"result"

      if( result.isInstanceOf[JsUndefined] )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right(Success(Some("Users added"), Some("ok")))
    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
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

    Logger.logger.debug("removeUsersToGroup: "+ jsonAdd.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonAdd,_,_)


    def handleJson(json:JsValue) = {
      val result = (json \ "result") \"result"

      if( result.isInstanceOf[JsUndefined] )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right(Success(Some("Users removed"), Some("ok")))
    }


    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }


  }

  def findUserByUid(userId: String):Future[Either[Error,IpaUser]]={

    //println("------------------->>>>"+MDC.get("user-id") )
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

    Logger.logger.debug("findUserByUid request: "+jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)


    def handleJson(json:JsValue) = {

      //println("------------------->>>>>>"+MDC.get("user-id") )

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val result = (json \ "result") \"result"

      if(count==0)
        Left( Error(Option(1),Some("No user found"),None) )

      if( result.isInstanceOf[JsUndefined] )

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


    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
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

    Logger.logger.debug("findUserByMail request: "+ jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)

    def handleJson(json:JsValue) = {

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val result = ((json \ "result") \"result")(0)//.getOrElse(JsString("null")).toString()

      if(count==0)
        Left( Error(Option(1),Some("No user found"),None) )

      else if( result.isInstanceOf[JsUndefined]  )
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

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
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

    Logger.logger.debug("findUserByMail request: "+ jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)


    def handleJson(json:JsValue) = {

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val result = (json \ "result") \"result"

      if(count==0)
        Left( Error(Option(1),Some("No organization founded"),None) )

      else if(  result.isInstanceOf[JsUndefined]  )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )

      else
        Right(
          result.asOpt[Seq[JsObject]].get map{ el => (el \"cn")(0).asOpt[String].get }
        )
    }


    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
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

    Logger.logger.info("login ckan")

    val loginInfo = new LoginInfo(userName,pwd,LoginClientLocal.CKAN)
    val wsResponse = loginClientLocal.login(loginInfo,wsClient)

    wsResponse.map(_=>"ok")
  }


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
        Option(out.head)
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