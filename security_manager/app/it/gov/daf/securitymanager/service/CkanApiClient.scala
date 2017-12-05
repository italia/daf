package it.gov.daf.securitymanager.service

import com.google.inject.Inject
import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo, SecuredInvocationManager}
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, Success}
import play.api.libs.functional.syntax._

import scala.concurrent.Future

class CkanApiClient @Inject()(secInvokeManager: SecuredInvocationManager, cacheWrapper:CacheWrapper){

  import scala.concurrent.ExecutionContext.Implicits._

  implicit val userFormat = Json.format[CkanOrgUser]
  implicit val orgFormat = Json.format[CkanOrg]

  /*
  private val ckanOrgUserWrites = new Writes[CkanOrgUser] {
    def writes(ckanOrgUser: CkanOrgUser) = Json.obj(
      "name" -> ckanOrgUser.name,
      "capacity" -> ckanOrgUser.role
    )
  }

  private implicit val ckanOrgUserReads: Reads[CkanOrgUser] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "capacity").read[String]
    )(CkanOrgUser.apply _)
  */

  private val ckanAdminLogin = new LoginInfo(ConfigReader.ckanAdminUser, ConfigReader.ckanAdminPwd, "ckan")

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

    println("createOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(ConfigReader.ckanHost + "/api/3/action/organization_create").withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageServiceCall(new LoginInfo(userName, userPwd, "ckan"), serviceInvoke) map ( evaluateResult(_,"Organization created") )

  }


  def deleteOrganization(groupCn:String):Future[Either[Error, Success]] = {

    val jsonRequest: JsValue = Json.parse( s"""{"id" : "$groupCn"}""" )

    println("deleteOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      val url = ConfigReader.ckanHost + "/api/3/action/organization_delete"
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageServiceCall(ckanAdminLogin, serviceInvoke) map ( evaluateResult(_,"Organization deleted") )

  }

  def purgeOrganization(groupCn:String):Future[Either[Error, Success]] = {

    val jsonRequest: JsValue = Json.parse( s"""{"id" : "$groupCn"}""" )

    println("purgeOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      val url = ConfigReader.ckanHost + "/api/3/action/organization_purge"
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    secInvokeManager.manageServiceCall(ckanAdminLogin, serviceInvoke) map ( evaluateResult(_,"Organization deleted") )

  }


  def putUserInOrganization(loggedUserName:String, userName:String, ckanOrg:CkanOrg):Future[Either[Error, Success]] = {

    /*
    val jsonUsers = Json.(orgUsers).toString()
    val jsonRequest: JsValue = Json.parse(
      s"""{
           "name": "$groupCn",
           "users": $jsonUsers
         }""")
    */

    val updatedCkanOrg = ckanOrg.copy( users=ckanOrg.users :+ CkanOrgUser(userName,"admin") )

    val jsonRequest: JsValue = Json.toJson(updatedCkanOrg)

    println("putUsersInOrganization request: " + jsonRequest.toString())


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(ConfigReader.ckanHost + "/api/3/action/organization_patch?id=" + updatedCkanOrg.name).withHeaders("Cookie" -> cookie).post(jsonRequest)
    }

    val ckanLogin =  new LoginInfo(loggedUserName, cacheWrapper.getPwd(loggedUserName).get , "ckan")
    secInvokeManager.manageServiceCall( ckanLogin, serviceInvoke) map ( evaluateResult(_,"User added") )

  }


  def getOrganization(loggedUserName:String, groupCn:String):Future[Either[Error, CkanOrg]] = {

    println("getUsersOfOrganization groupCn: " + groupCn)


    def serviceInvoke( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = ConfigReader.ckanHost  + "/api/3/action/organization_show?id=" + groupCn
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }


    val ckanLogin =  new LoginInfo(loggedUserName, cacheWrapper.getPwd(loggedUserName).get , "ckan")

    secInvokeManager.manageServiceCall(ckanLogin, serviceInvoke) map getOrgFromJson

  }


  private def evaluateResult(json:JsValue, okMessage:String ):Either[Error, Success]={

    val resultJson = (json \ "success").toOption

    if( !resultJson.isEmpty && resultJson.get.toString().equals("true") )
      Right( Success(Some(okMessage), Some("ok")) )
    else
      Left(Error(Option(0), Some(WebServiceUtil.getMessageFromCkanError(json) ), None))

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

  /*
  private def getUsersFromJson(json:JsValue):Either[Error, Seq[CkanOrgUser]]={

    val resultJson = (json \ "success").toOption

    if( !resultJson.isEmpty && resultJson.get.toString() == "true" ) {

      ((json \ "result") \ "users").validate[Seq[CkanOrgUser]] match{
        case s: JsSuccess[Seq[CkanOrgUser]] => Right(s.get)
        case e: JsError => Left( Error(Option(0), Some(WebServiceUtil.getMessageFromJsError(e) ), None) )
      }

    }else
      Left(Error(Option(0), Some(WebServiceUtil.getMessageFromCkanError(json) ), None))

  }*/


}

case class CkanOrgUser(name:String,capacity:String)
case class CkanOrg(name:String, id:String, users:Seq[CkanOrgUser])

