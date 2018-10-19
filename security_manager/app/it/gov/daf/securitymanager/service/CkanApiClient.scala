package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.Inject
import it.gov.daf.common.sso.common._
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.utilities.ConfigReader
import it.gov.daf.sso.LoginClientLocal
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, Success}
import cats.implicits._

import scala.concurrent.Future
import it.gov.daf.common.sso.common.{Admin, Editor, Viewer}

import scala.annotation.switch



class CkanApiClient @Inject()(secInvokeManager: SecuredInvocationManager, cacheWrapper:CacheWrapper){

  import play.api.libs.concurrent.Execution.Implicits._

  implicit val userFormat = Json.format[CkanOrgUser]
  implicit val orgFormat = Json.format[CkanOrg]


  //private val ckanAdminLogin = new LoginInfo(ConfigReader.ckanAdminUser, ConfigReader.ckanAdminPwd, LoginClientLocal.CKAN)
  private val ckanGeoAdminLogin = new LoginInfo(ConfigReader.ckanGeoAdminUser, ConfigReader.ckanGeoAdminPwd, LoginClientLocal.CKAN_GEO)

  def createOrganization(userName:String, userPwd:String, groupCn:String):Future[Either[Error, Success]] = {

    val jsonRequest: JsValue = Json.parse(
      s"""{
           "description": "Organizzazione: $groupCn",
           "title": "Organizzazione: $groupCn",
           "name": "$groupCn",
           "is_organization": true,
           "state": "active",
           "type": "organization"
         }""")

    Logger.logger.debug("createOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(ConfigReader.ckanHost + "/api/3/action/organization_create").withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall(new LoginInfo(userName, userPwd, "ckan"), serviceInvoke, 200) map ( evaluateResult(_,"Organization created") )

  }

  /*
  def createOrganizationAsAdmin(groupCn:String):Future[Either[Error, Success]] = {
    createOrganizationAsAdmin(false,groupCn)
  }*/

  def createOrganizationInGeoCkanAsAdmin(groupCn:String):Future[Either[Error, Success]] = {
    createOrganizationAsAdmin(true,groupCn)
  }

  private type CkanInfo = (String,LoginInfo)
  private def selectInfo(isGeoCkan:Boolean):CkanInfo =  if(isGeoCkan) (ConfigReader.ckanGeoHost, ckanGeoAdminLogin)
                                                        else throw new Exception("Only ckan geo is permitted")//else (ConfigReader.ckanHost,ckanAdminLogin)

  private def createOrganizationAsAdmin(isGeoCkan:Boolean,groupCn:String):Future[Either[Error, Success]] = {

    val ckaninfo = selectInfo(isGeoCkan)


    val jsonRequest: JsValue = Json.parse(
      s"""{
           "description": "Organizzazione: $groupCn",
           "title": "Organizzazione: $groupCn",
           "name": "$groupCn",
           "is_organization": true,
           "state": "active",
           "type": "organization"
         }""")

    Logger.logger.debug("createOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      println("XXX_>"+ckaninfo._1 + "/api/3/action/organization_create" + "  Cookie:"+cookie)
      wsClient.url(ckaninfo._1 + "/api/3/action/organization_create").withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall(ckaninfo._2, serviceInvoke, 200) map ( evaluateResult(_,s"Organization created (${ckaninfo._1})") )

  }

  /*
  def deleteOrganization(groupCn:String):Future[Either[Error, Success]] = {
    deleteOrganization(false,groupCn)
  }*/

  def deleteOrganizationInGeoCkan(groupCn:String):Future[Either[Error, Success]] = {
    deleteOrganization(true,groupCn)
  }

  private def deleteOrganization(isGeoCkan:Boolean,groupCn:String):Future[Either[Error, Success]] = {

    val ckaninfo = selectInfo(isGeoCkan)

    val jsonRequest: JsValue = Json.parse( s"""{"id" : "$groupCn"}""" )

    Logger.logger.debug("deleteOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      val url = ckaninfo._1 + "/api/3/action/organization_delete"
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall(ckaninfo._2, serviceInvoke, 200) map ( evaluateResult(_,s"Organization deleted (${ckaninfo._1})") )

  }

  /*
  def deleteUser(userUid:String):Future[Either[Error, Success]] = {

    val jsonRequest: JsValue = Json.parse( s"""{"id" : "$userUid"}""" )

    Logger.logger.debug("deleteUser request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      val url = ConfigReader.ckanHost + "/api/3/action/user_delete"
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall(ckanAdminLogin, serviceInvoke, 200) map ( evaluateResult(_,"User deleted") )

  }*/

  /*
  def purgeOrganization(groupCn:String):Future[Either[Error, Success]] = {
    purgeOrganization(false, groupCn)
  }*/

  def purgeOrganizationInGeoCkan(groupCn:String):Future[Either[Error, Success]] = {
    purgeOrganization(true, groupCn)
  }

  private def purgeOrganization(isGeoCkan:Boolean, groupCn:String):Future[Either[Error, Success]] = {

    val ckaninfo = selectInfo(isGeoCkan)

    val jsonRequest: JsValue = Json.parse( s"""{"id" : "$groupCn"}""" )

    Logger.logger.debug("purgeOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      val url = ckaninfo._1 + "/api/3/action/organization_purge"
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall(ckaninfo._2, serviceInvoke, 200) map ( evaluateResult(_,s"Organization purged (${ckaninfo._1})") )

  }

  /*
    def putUserInOrganization(loggedUserName:String, userName:String, ckanOrg:CkanOrg):Future[Either[Error, Success]] = {

      val updatedCkanOrg = ckanOrg.copy( users=ckanOrg.users :+ CkanOrgUser(userName,"admin") )

      val jsonRequest: JsValue = Json.toJson(updatedCkanOrg)

      Logger.logger.debug("putUsersInOrganization request: " + jsonRequest.toString())


      def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
        wsClient.url(ConfigReader.ckanHost + "/api/3/action/organization_patch?id=" + updatedCkanOrg.name).withHeaders("Cookie" -> cookie).post(jsonRequest)
      }

      val ckanLogin =  new LoginInfo(loggedUserName, cacheWrapper.getPwd(loggedUserName).get , "ckan")
      secInvokeManager.manageRestServiceCall( ckanLogin, serviceInvoke, 200) map ( evaluateResult(_,"User added") )

    }


    def putUserInOrganizationAsAdmin(userName:String, ckanOrg:CkanOrg):Future[Either[Error, Success]] = {

      val updatedCkanOrg = ckanOrg.copy( users=ckanOrg.users :+ CkanOrgUser(userName,"admin") )

      val jsonRequest: JsValue = Json.toJson(updatedCkanOrg)

      Logger.logger.debug("putUsersInOrganization request: " + jsonRequest.toString())


      def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
        wsClient.url(ConfigReader.ckanHost + "/api/3/action/organization_patch?id=" + updatedCkanOrg.name).withHeaders("Cookie" -> cookie).post(jsonRequest)
      }

      secInvokeManager.manageRestServiceCall( ckanAdminLogin, serviceInvoke, 200) map ( evaluateResult(_,"User added") )

    }*/

  /*
  def putUserInOrganizationAsAdminInGeoCkan(userName:String, ckanOrg:CkanOrg):Future[Either[Error, Success]] = {

    val updatedCkanOrg = ckanOrg.copy( users=ckanOrg.users :+ CkanOrgUser(userName,"admin") )

    val jsonRequest: JsValue = Json.toJson(updatedCkanOrg)

    Logger.logger.debug("putUsersInOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(ConfigReader.ckanGeoHost + "/api/3/action/organization_patch?id=" + updatedCkanOrg.name).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall( ckanGeoAdminLogin, serviceInvoke, 200) map ( evaluateResult(_,"User added") )

  }*/


  def putUserInOrganizationAsAdminInGeoCkan(userName:String, ckanOrg:CkanOrg, role:Role):Future[Either[Error, Success]] = {

    val ckanRole = role match{
      case Admin => "admin"
      case Editor => "editor"
      case Viewer => "member"
    }

    val updatedCkanOrg = ckanOrg.copy( users=ckanOrg.users :+ CkanOrgUser(userName,ckanRole) )

    val jsonRequest: JsValue = Json.toJson(updatedCkanOrg)

    Logger.logger.debug("putUsersInOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(ConfigReader.ckanGeoHost + "/api/3/action/organization_patch?id=" + updatedCkanOrg.name).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall( ckanGeoAdminLogin, serviceInvoke, 200) map ( evaluateResult(_,"User added") )

  }


  /*
  def removeUserInOrganizationAsAdmin(userName:String, ckanOrg:CkanOrg):Future[Either[Error, Success]] = {

    val updatedCkanOrg = ckanOrg.copy( users=ckanOrg.users.filter(c=>(!c.name.equals(userName))) )

    val jsonRequest: JsValue = Json.toJson(updatedCkanOrg)

    Logger.logger.debug("removeUsersInOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(ConfigReader.ckanHost + "/api/3/action/organization_patch?id=" + updatedCkanOrg.name).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall( ckanAdminLogin, serviceInvoke, 200) map ( evaluateResult(_,"User removed") )

  }*/

  def removeUserInOrganizationAsAdminInGeoCkan(userName:String, ckanOrg:CkanOrg):Future[Either[Error, Success]] = {

    val updatedCkanOrg = ckanOrg.copy( users=ckanOrg.users.filter(c=>(!c.name.equals(userName))) )

    val jsonRequest: JsValue = Json.toJson(updatedCkanOrg)

    Logger.logger.debug("removeUsersInOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(ConfigReader.ckanGeoHost + "/api/3/action/organization_patch?id=" + updatedCkanOrg.name).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageRestServiceCall( ckanGeoAdminLogin, serviceInvoke, 200) map ( evaluateResult(_,"User removed") )

  }

  /*
  def getOrganization(loggedUserName:String, groupCn:String):Future[Either[Error, CkanOrg]] = {

    Logger.logger.info("getUsersOfOrganization groupCn: " + groupCn)


    def serviceInvoke( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = ConfigReader.ckanHost  + "/api/3/action/organization_show?id=" + groupCn
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }


    val ckanLogin =  new LoginInfo(loggedUserName, cacheWrapper.getPwd(loggedUserName).get , "ckan")

    secInvokeManager.manageRestServiceCall(ckanLogin, serviceInvoke, 200) map getOrgFromJson

  }*/

/*
  def getOrganizationAsAdmin( groupCn:String):Future[Either[Error, CkanOrg]] = {

    Logger.logger.info("getUsersOfOrganization groupCn: " + groupCn)


    def serviceInvoke( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = ConfigReader.ckanHost  + "/api/3/action/organization_show?id=" + groupCn
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokeManager.manageRestServiceCall(ckanAdminLogin, serviceInvoke, 200) map getOrgFromJson

  }*/


  def getOrganizationAsAdminInGeoCkan( groupCn:String):Future[Either[Error, CkanOrg]] = {

    Logger.logger.info("getUsersOfOrganization groupCn: " + groupCn)


    def serviceInvoke( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = ConfigReader.ckanGeoHost  + "/api/3/action/organization_show?id=" + groupCn
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokeManager.manageRestServiceCall(ckanGeoAdminLogin, serviceInvoke, 200) map getOrgFromJson

  }


  def addUserToOrgsInGeoCkan(dafRoles:Option[Seq[String]], userName:String):Future[Either[Error,Success]] = {

    Logger.logger.debug(s"addUserToOrgsInGeoCkan dafRoles: $dafRoles userName $userName")

    handleMultipleCalls( dafRoles, addUserWithRoleInGeoCkan(_,userName) )
  }

  def removeUserFromOrgsInGeoCkan(dafRoles:Option[Seq[String]], userName:String):Future[Either[Error,Success]] = {

    handleMultipleCalls( dafRoles, removeUserWithRoleInGeoCkan(_,userName) )
  }

  private def handleMultipleCalls(dafRoles:Option[Seq[String]], fx:(String)=>Future[Either[Error,Success]]):Future[Either[Error,Success]]= {


    dafRoles match{

      case Some(roles) =>

        val traversed : Future[List[Either[Error, Success]]] = roles.toList.traverse(fx(_))

        traversed.map { list =>

          val start = Right(Success(Some("ok"), Some("ok")))
          list.foldLeft[Either[Error, Success]](start)( (a, b) => a match {
            case Right(r) => b
            case _ => a
          })

        }

      case _ => Future.successful{ Right(Success(Some("ok"), Some("ok"))) }

    }


  }

  private def addUserWithRoleInGeoCkan(roleString:String, userName:String):Future[Either[Error,Success]]={

    val roleOnOrg = getRoleAndOrgFromRoleString(roleString)
    addUserToOrgInGeoCkan(roleOnOrg._2,userName,roleOnOrg._1)

  }

  private def removeUserWithRoleInGeoCkan(roleString:String, userName:String):Future[Either[Error,Success]]={
    val roleOnOrg = getRoleAndOrgFromRoleString(roleString)
    removeUserFromOrgInGeoCkan(roleOnOrg._2,userName)
  }

  private def getRoleAndOrgFromRoleString(roleString:String):(Role,String)={

    Logger.logger.debug(s"getRoleAndOrgFromRoleString in roleString: $roleString")

    val prefix = roleString.substring(0,8)
    val admin = Admin.toString
    val editor = Editor.toString
    val viewer = Viewer.toString

    val role = prefix match{
      case `admin` => Admin
      case `editor` => Editor
      case `viewer` => Viewer
    }

    val org = roleString.substring(8)

    Logger.logger.debug(s"getRoleAndOrgFromRoleString out: role: $role org: $org")

    (role,org)
  }

  def addUserToOrgInGeoCkan(org:String, userName:String, role:Role):Future[Either[Error,Success]]= {

    val result = for {

      org <- EitherT( getOrganizationAsAdminInGeoCkan(org) )
      a <- EitherT(  putUserInOrganizationAsAdminInGeoCkan(userName,org,role) )

    } yield a

    result.value.map{
      case Right(r) => Right( Success(Some("Ckan org modified"), Some("ok")) )
      case Left(l) => Left(l)
    }

  }

  def removeUserFromOrgInGeoCkan(org:String, userName:String):Future[Either[Error,Success]]= {

    val result = for {

      org <- EitherT( getOrganizationAsAdminInGeoCkan(org) )
      a <- EitherT(  removeUserInOrganizationAsAdminInGeoCkan(userName,org) )

    } yield a

    result.value.map{
      case Right(r) => Right( Success(Some("Ckan org modified"), Some("ok")) )
      case Left(l) => Left(l)
    }

  }


  private def evaluateResult(jsonE:Either[String,JsValue], okMessage:String):Either[Error, Success]={

    jsonE match {
      case Right(json) => evaluateResult(json, okMessage)
      case Left(msg) => Left( Error(Option(0), Some(msg), None) )
    }

  }

  private def evaluateResult(json:JsValue, okMessage:String):Either[Error, Success]={

      val resultJson = (json \ "success").toOption

      if( !resultJson.isEmpty && resultJson.get.toString().equals("true") )
        Right( Success(Some(okMessage), Some("ok")) )
      else
        Left(Error(Option(0), Some(WebServiceUtil.getMessageFromCkanError(json) ), None))


  }

  private def getOrgFromJson(jsonE:Either[String,JsValue]):Either[Error, CkanOrg]={

    jsonE match {
      case Right(json) => getOrgFromJson(json)
      case Left(msg) =>Left(Error(Option(0),Some( msg ), None))
    }

  }

  private def getOrgFromJson(json:JsValue):Either[Error, CkanOrg]={

    val resultJson = (json \ "success").toOption

    if( !resultJson.isEmpty && resultJson.get.toString() == "true" ) {

      (json \ "result").validate[CkanOrg] match{
        case s: JsSuccess[CkanOrg] => Right(s.get)
        case e: JsError => Left( Error(Option(0), Some(WebServiceUtil.getMessageFromJsError(e) ), None) )
      }

    }else
      Left(Error(Option(0), Some(WebServiceUtil.getMessageFromCkanError(json) ), None))

  }



}

case class CkanOrgUser(name:String,capacity:String)
case class CkanOrg(name:String, id:String, users:Seq[CkanOrgUser])

