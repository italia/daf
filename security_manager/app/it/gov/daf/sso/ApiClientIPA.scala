package it.gov.daf.sso

import java.net.URLEncoder

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{LoginInfo, Role, SecuredInvocationManager}
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.service.IntegrationService
import it.gov.daf.securitymanager.utilities.{AppConstants, ConfigReader}
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}
import security_manager.yaml.{DafGroupInfo, Error, GroupList, IpaGroup, IpaUser, Success}
import cats.implicits._
import it.gov.daf.securitymanager.service
import it.gov.daf.securitymanager.service.ProcessHandler._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger

import scala.util.Try

@Singleton
class ApiClientIPA @Inject()(secInvokeManager:SecuredInvocationManager,loginClientLocal: LoginClientLocal,wsClient: WSClient){

  private val loginInfo = new LoginInfo(ConfigReader.ipaUser, ConfigReader.ipaUserPwd, LoginClientLocal.FREE_IPA)

  private val logger = Logger(this.getClass.getName)


  def testH:Future[Either[Error,Success]]={
    wsClient.url("https://master:50470/webhdfs/v1/app?op=GETFILESTATUS").withAuth("krbtgt/DAF.GOV.IT@DAF.GOV.IT","andreacherici@DAF.GOV.IT", WSAuthScheme.SPNEGO).get().map{ resp =>
      println("----->>>"+resp.body)
      Left( Error(Option(0),Some("wee"),None) )
    }
  }


  // only sysadmin and ipaAdmin
  def createUser(user: IpaUser, isReferenceUser:Boolean):Future[Either[Error,Success]]= {


    val titleAttributeValue = if(isReferenceUser) AppConstants.ReferenceUserTitle else AppConstants.UserTitle

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

    logger.debug("createUser: "+jsonUser.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonUser,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result").getOrElse(JsString("null")).toString()

      if(result != "null") {

        val randomPwd = (((json \ "result")\"result")\"randompassword").asOpt[String]
        randomPwd match {
          case Some(rpwd) => Future.successful( Right(Success(Some("User created"), randomPwd)) )
          case None => Future.successful {
            Left(Error(Option(0), Some(readIpaErrorMessage(json)), None))
          }
        }

        /*
        val randomPwd = (((json \ "result")\"result")\"randompassword").asOpt[String]
        randomPwd match{
          case Some(rpwd) => loginCkanGeo(user.uid, rpwd).map ( _ => Right(Success(Some("User created"), randomPwd)) )
          case None => Future{Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )}
        }*/

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

    logger.debug("updateUser :"+jsonUser.toString())

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

    logger.debug("resetPwd: "+jsonUser.toString())

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

    logger.debug("passwd: "+jsonUser.toString())

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

    logger.debug(s"login IPA (changePassword): user=$userUid")

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

    logger.debug("deleteUser: "+jsonDelete.toString())

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
  def createGroup(group:Group, parentGroup:Option[Group]):Future[Either[Error,Success]]= {

    val parentGroupNames = group match{
      case Organization(_) => Some(Seq(ORGANIZATIONS_GROUP))
      case WorkGroup(_) => parentGroup match{
                                              case Some(x) => Some(Seq(WORKGROUPS_GROUP,x.toString))
                                              case None => None// must raise an error (see testEmptyness)
                                            }
      case RoleGroup(_) => Some(Seq(ROLES_GROUP))
    }

    def testEmptyness:Future[Either[Error,Success]]=if(parentGroupNames.nonEmpty)
                                                      Future.successful{Right(Success(Some("ok"), Some("ok")))}
                                                    else
                                                      Future.successful{Left(Error(Option(0), Some("Workgroups needs a parent organization"), None))}

    val out = for{
      we <- stepOver( testEmptyness )
      a <- step( createGroup(group.toString) )
      b <- stepOver( a, addMemberToGroups(parentGroupNames, group) )
    }yield b


    out.value.map{
      case Right(r) => Right( Success(Some("Group created"), Some("ok")) )
      case Left(l) => if(l.steps !=0) {
        deleteGroup(group.toString).onSuccess { case e =>
          if(e.isLeft)
            throw new Exception( s"createGroup rollback issue" )
        }

      }
        Left(l.error)
    }

  }


  private def createGroup(group: String):Future[Either[Error,Success]]= {

    val jsonGroup: JsValue = Json.parse(
                                s"""{
                                       "method":"group_add",
                                       "params":[
                                          [
                                             "$group"
                                          ],
                                          {
                                             "description":"${AppConstants.IpaGroupDescription}",
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    logger.debug("createGroup: "+ jsonGroup.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonGroup,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result") \"result"

      println("----->"+result)

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


  private def groupInfo(groupName:String, orgListFuture:Future[Either[Error,Seq[String]]]):Future[Either[Error,DafGroupInfo]]={

    val result = for{
      wrk <- EitherT( showGroup(groupName) )
      orgList <- EitherT( orgListFuture )
    } yield (ApiClientIPA.extractGroupsOf(wrk.memberof_group,orgList), wrk.member_group, wrk.memberof_group)

    result.value map{
      /*
      case Right( (Some(Seq(orgs)),_,_) ) => Right( DafGroupInfo(groupName, "Workgroup", Option(orgs), None) )
      case Right( (_,Some(wrks),Some(parentGroups)) )  => logger.debug(s"parentGroups $parentGroups");
                                                          if(parentGroups.contains(ORGANIZATIONS_GROUP)) Right( DafGroupInfo(groupName, "Organization", None, Option(wrks)) )
                                                          else Right( DafGroupInfo(groupName, "Generic Group", None, None) )
      case Right( (_,_,_) )  => Right( DafGroupInfo(groupName, "Generic Group", None, None) )
      */

      case Right( (orgs, memberGroups, Some(parentGroups)) )=>  if(parentGroups.contains(ORGANIZATIONS_GROUP)) Right( DafGroupInfo(groupName, "Organization", None, memberGroups) )
                                                                else if (parentGroups.contains(WORKGROUPS_GROUP)) Right( DafGroupInfo(groupName, "Workgroup", orgs.map(_.head), None) )
                                                                else Right( DafGroupInfo(groupName, "Generic Group", None, None) )
      case Right( (_,_,_) ) => Right( DafGroupInfo(groupName, "Generic Group", None, None) )
      case Left(l) => Left(l)

    }

  }

  // all users
  def groupsInfo(groups: Seq[String]):Future[Either[Error,Seq[DafGroupInfo]]]= {

    val orgList:Future[Either[Error,Seq[String]]] = organizationList()

    val traversed = groups.toList.traverse[Future, Either[Error, DafGroupInfo]](groupInfo(_,orgList)): Future[List[Either[Error, DafGroupInfo]]]

    traversed.map{ tList=>

      val out = tList.foldLeft(List[DafGroupInfo]())((a, b) => b match {
        case Right(r) => r :: a
        case _ => a
      })
      Right(out.reverse.toSeq)

    }

  }


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

    logger.debug("showGroup: "+ jsonGroup.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonGroup,_,_)

    def handleJson(json:JsValue) = {
      val result = (json \ "result") \"result"

      if( result.isInstanceOf[JsUndefined] )

        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else
        Right(
          IpaGroup(
            (result \ "dn").asOpt[String].getOrElse(""),
            (result \ "memberof_group").asOpt[Seq[String]],
            (result \ "member_user").asOpt[Seq[String]].map{ _.filter( a=> a.endsWith(service.ORG_REF_USER_POSTFIX) || a.endsWith(service.WRK_REF_USER_POSTFIX) )},//sys users
            (result \ "member_group").asOpt[Seq[String]],
            (result \ "gidnumber") (0).asOpt[String],
            (result \ "member_user").asOpt[Seq[String]].map{ _.filterNot( a=> a.endsWith(service.ORG_REF_USER_POSTFIX) || a.endsWith(service.WRK_REF_USER_POSTFIX) )}//users
          )
        )
    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }


  }

  def testGroupForCreation(groupCn:String):Future[Either[Error,Success]] ={

    showGroup(groupCn) map{
      case Right(r) =>  Left(Error(Option(1),Some("There is already a group with the same name"),None))

      case Left(l) => Right(Success(Some("ok"), Some("ok")))
    }

  }


  def testGroupForDeletion(groupCn:Group):Future[Either[Error,Success]] ={


    //TODO to rework..
    val groupEither:Either[Organization,WorkGroup] = groupCn match{
      case o:Organization => Left(o)
      case w:WorkGroup => Right(w)
    }

    showGroup(groupCn.toString) map{
      case Right(r) =>  if( r.member_user.nonEmpty && r.member_user.get.nonEmpty)
                          Left(Error(Option(1),Some("This group contains users"),None))
                        else if( r.member_group.nonEmpty && r.member_group.get.nonEmpty )
                          Left(Error(Option(1),Some("This group contains other groups"),None))
                        else if( groupEither.isLeft && isOrganization(r).isLeft )
                          Left(Error(Option(1),Some("Not an organization"),None))
                        else if( groupEither.isRight && isWorkgroup(r).isLeft )
                          Left(Error(Option(1),Some("Not an workgroup"),None))
                        else
                          Right(Success(Some("Empty group"), Some("ok")))

      case Left(l) => Left(l)
    }

  }

  def testIfIsWorkgroup(groupCn:String):Future[Either[Error,Success]] ={

    showGroup(groupCn) map{
      case Right(r) =>  isWorkgroup(r)
      case Left(l) => Left(l)
    }

  }

  def testIfIsOrganization(groupCn:String):Future[Either[Error,Success]] ={

    showGroup(groupCn) map{
      case Right(r) =>  isOrganization(r)
      case Left(l) => Left(l)
    }

  }


  def testIfUserBelongsToOrgWrk(orgName:String,userName:String):Future[Either[Error,Success]] ={

    val result = for{
      userInfo <- EitherT( findUser(Left(userName)) )
      orgInfo <- EitherT( showGroup(orgName) )
    } yield ApiClientIPA.extractGroupsOf( userInfo.workgroups, orgInfo.member_group.getOrElse(Seq.empty) )

    result.value map{
      case Right(None) => Right(Success(Some("ok"), Some("ok")))
      case Right(Some(x)) =>  if(x.nonEmpty) Left(Error(Option(1),Some("This user belongs to organization workgroups"),None))
                              else Right(Success(Some("ok"), Some("ok")))
      case Left(l) => Left(l)
    }

  }


  def getWorkgroupOrganization(wrk:IpaGroup):Future[Either[Error,Option[String]]]={

    val result = for{
      orgList <- EitherT( organizationList )
    } yield ApiClientIPA.extractGroupsOf(wrk.memberof_group,orgList)

    result.value map{
      case Right(Some(Seq(x))) => Right(Option(x))
      case Left(l) => Left(l)
      case _ => Right(None)
    }

  }

  def isWorkgroup(ipaGroup:IpaGroup) = {

    if( ipaGroup.memberof_group.getOrElse(List.empty[String]).contains(WORKGROUPS_GROUP) )
      Right(Success(Some("Ok"), Some("ok")))
    else
      Left(Error(Option(1),Some("Not a daf workgroup"),None))

  }

  private def isOrganization(ipaGroup:IpaGroup)={

    val groups= ipaGroup.memberof_group.getOrElse(List.empty[String])

    if( groups.contains(ORGANIZATIONS_GROUP) && !groups.contains(WORKGROUPS_GROUP) )
        Right(Success(Some("Ok"), Some("ok")))
    else
        Left(Error(Option(1),Some("Not an organization"),None))

  }

  private def isOrganizationRole(ipaGroup:IpaGroup)={

    val groups= ipaGroup.memberof_group.getOrElse(List.empty[String])

    if( groups.contains(ORGANIZATIONS_GROUP) && groups.contains(ROLES_GROUP) )
      Right(Success(Some("Ok"), Some("ok")))
    else
      Left(Error(Option(1),Some("Not an organization role"),None))

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

    logger.debug("deleteGroup: "+jsonDelete.toString())

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

  private def handleGroupsMemberships(groups:Option[Seq[String]], member:Member, fx:(String, Seq[Member])=>Future[Either[Error,Success]]):Future[Either[Error,Success]]= {

    //val traversed = groups.toList.traverse[Future, Either[Error, Long]](findRoleId): Future[List[Either[Error, Long]]]

    groups match {

      case Some(glist) =>

        val traversed : Future[List[Either[Error, Success]]] = glist.toList.traverse(fx(_,Seq(member)))

        traversed.map { list =>

          val start = Right(Success(Some("ok"), Some("ok")))
          list.foldLeft[Either[Error, Success]](start)((a, b) => a match {
            case Right(r) => b
            case _ => a
          })

        }

      case _ => Future.successful( Right(Success(Some("ok"), Some("ok"))) )

    }


  }

  def addMemberToGroups(groups:Option[Seq[String]], member:Member):Future[Either[Error,Success]] = handleGroupsMemberships(groups, member, addMembersToGroup)

  def addMembersToGroup(group: String, memberList: Member*):Future[Either[Error,Success]]= {

    val jArrayStr = memberList.mkString("\"","\",\"","\"")

    val memberType = memberList match {
      case Seq(User(_)) => "user"
      case _ => "group"
    }

    val jsonAdd: JsValue = Json.parse(
      s"""{
                                       "method":"group_add_member",
                                       "params":[
                                          [
                                             "$group"
                                          ],
                                          {
                                             "$memberType":[$jArrayStr],
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    logger.debug("addUsersToGroup: "+ jsonAdd.toString())

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

  def removeMemberFromGroups(groups:Option[Seq[String]], member:Member):Future[Either[Error,Success]] = handleGroupsMemberships(groups, member, removeMembersFromGroup)

  def removeMembersFromGroup(group: String, memberList: Member*):Future[Either[Error,Success]]= {

    val jArrayStr = memberList.mkString("\"","\",\"","\"")

    val memberType = memberList match {
      case Seq(User(_)) => "user"
      case _ => "group"
    }

    val jsonAdd: JsValue = Json.parse(
                                    s"""{
                                       "method":"group_remove_member",
                                       "params":[
                                          [
                                             "$group"
                                          ],
                                          {
                                             "$memberType":[$jArrayStr],
                                             "raw":false,
                                             "version": "2.213"
                                          }
                                       ],
                                       "id":0
                                    }""")

    logger.debug("removeUsersToGroup: "+ jsonAdd.toString())

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


  type UserId = String
  type Mail = String
  private def performFindUser( param:Either[UserId,Mail], wrkGroups:Seq[String], orgs:Seq[String] ):Future[Either[Error,IpaUser]] ={

    val jsonRequest:JsValue = param match{

      case Right(mail) => Json.parse(s"""{
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

      case Left(userId) => Json.parse(s"""{
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
    }


    logger.debug("performFindUser request: "+jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)


    def handleJson(json:JsValue) = {

      //println("------------------->>>>>>"+MDC.get("user-id") )

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val tempoResult = (json \ "result") \"result"

      if(count==0)
        Left( Error(Option(1),Some("No user found"),None) )
      else if( tempoResult.isInstanceOf[JsUndefined] )
        Left( Error(Option(0),Some(readIpaErrorMessage(json)),None) )
      else {

        val result:JsLookupResult = param match {
          case Right(mail) => tempoResult(0)
          case Left(userId) => tempoResult
        }

        Right(
          IpaUser(
            sn = (result \ "sn") (0).asOpt[String].getOrElse(""),
            givenname = (result \ "givenname") (0).asOpt[String].getOrElse(""),
            mail = (result \ "mail") (0).asOpt[String].getOrElse(""),
            uid = (result \ "uid") (0).asOpt[String].getOrElse(""),
            roles = ApiClientIPA.extractRole((result \ "memberof_group").asOpt[Seq[String]]),
            workgroups = ApiClientIPA.extractGroupsOf((result \ "memberof_group").asOpt[Seq[String]], wrkGroups),
            title = (result \ "title") (0).asOpt[String],
            userpassword = None,
            organizations = ApiClientIPA.extractGroupsOf((result \ "memberof_group").asOpt[Seq[String]], orgs)
          )
        )
      }

    }

    secInvokeManager.manageRestServiceCall(loginInfo,serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }

  }


  def findUser(param:Either[UserId,Mail]):Future[Either[Error,IpaUser]]= {

    def listThis(fx: => Future[Either[Error, Seq[String]]]): Future[Either[Error, Seq[String]]] = {
      fx.map {
        case Left(Error(Some(1), Some(x), None)) => Right(Seq.empty[String])//println("1--->"+x);if(num==1)Right(Seq.empty[String]) else Left(Error(Some(num), Some(x), None))
        case Left(x) => Left(x)
        case Right(x) => Right(x)
      }

    }

    val result = for{
      orgs <- EitherT( listThis(organizationList) )
      wrks <- EitherT( listThis(workgroupList) )
      out <- EitherT( performFindUser(param,wrks,orgs) )
    } yield out

    result.value
  }


  def organizationList():Future[Either[Error,Seq[String]]] = groupList( ORGANIZATIONS_GROUP,Some(WORKGROUPS_GROUP) )
  def workgroupList():Future[Either[Error,Seq[String]]] = groupList(WORKGROUPS_GROUP)
  def roleList():Future[Either[Error,Seq[String]]] = groupList(ROLES_GROUP)

  private def groupList(memberOf:String,notMemberOf:Option[String]=None):Future[Either[Error,Seq[String]]]={

    val notMemberOfCondition = notMemberOf match{
                                  case Some(x) => s""""not_in_group": "$x", """
                                  case None => ""
                                }

    val jsonRequest:JsValue = Json.parse(s"""{
                                             "id": 0,
                                             "method": "group_find",
                                             "params": [
                                                 [""],
                                                 {
                                                    "in_group": "${memberOf}", $notMemberOfCondition
                                                    "all": "false",
                                                    "raw": "true",
                                                    "version": "2.213"
                                                 }
                                             ]
                                         }""")

    logger.debug("groupList request: "+ jsonRequest.toString())

    val serviceInvoke : (String,WSClient)=> Future[WSResponse] = callIpaUrl(jsonRequest,_,_)


    def handleJson(json:JsValue) = {

      val count = ((json \ "result") \ "count").asOpt[Int].getOrElse(-1)
      val result = (json \ "result") \"result"

      if(count==0)
        Left( Error(Option(1),Some("No groups founded"),None) )

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


  def loginCkanGeo(userName:String, pwd:String):Future[Either[Error,Success]] = {

    logger.info("login ckan geo")

    val loginInfo = new LoginInfo(userName,pwd,LoginClientLocal.CKAN_GEO)
    val wsResponse = Try{ loginClientLocal.login(loginInfo,wsClient) }

    wsResponse match{
      case scala.util.Success(s) => s.map( _=>Right(Success(Some("ok"), Some("ok"))) )
      case scala.util.Failure(f) => logger.error(f.getMessage,f);Future.successful( Left(Error(Option(0),Some(f.getMessage),None)) )
    }
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

  def extractRole( in:Option[Seq[String]] ): Option[Seq[String]] = {

    if(in.isEmpty)
      None
    else
      Some( in.get.filter(group => Role.rolesPrefixs.exists(group.startsWith _)) )

  }

  def extractGroupsOf( groups:Option[Seq[String]], of:Seq[String] ): Option[Seq[String]] = {

    if(groups.isEmpty)
      None
    else
      Option( groups.get.filter(group => of.contains(group)) )
  }

}


sealed abstract class Member{
  def value:String
  override def toString = value
}

sealed abstract class Group extends Member

case class User(value: String) extends Member{
  //override def toString = value
}
case class Organization(value: String) extends Group{
  //override def toString = value
}

case class WorkGroup(value: String) extends Group{
  //override def toString = value
}

case class RoleGroup(value: String) extends Group{
  //override def toString = value
}

