package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.sso.ApiClientIPA
import play.api.libs.json._
import security_manager.yaml.IpaUser
//import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{DafOrg, Error, IpaUser, Success, UserList}

import scala.concurrent.Future
import cats.implicits._
import org.apache.commons.lang3.StringEscapeUtils

@Singleton
class IntegrationService @Inject()(apiClientIPA:ApiClientIPA,secInvokeManager:SecuredInvocationManager){

  import scala.concurrent.ExecutionContext.Implicits._

  private val loginInfoSuperset = new LoginInfo(ConfigReader.suspersetAdminUser,ConfigReader.suspersetAdminPwd,"superset")

  private def toDbName(groupCn:String)=s"$groupCn-db"
  private def toRoleName(groupCn:String)=s"datarole-$groupCn-db"
  private def toUserName(groupCn:String)=s"$groupCn-default-admin"
  private def toMail(groupCn:String)=s"$groupCn@default.it"

  def createDafOrganization(dafOrg:DafOrg):Future[Either[Error,Success]] = {


    val defaultOrgIpaUser = new IpaUser(  dafOrg.groupCn,
                                          "default org admin",
                                          toMail(dafOrg.groupCn),
                                          toUserName(dafOrg.groupCn),
                                          Option(Role.Admin.toString),
                                          Option(dafOrg.defaultUserPwd),
                                          Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- EitherT( apiClientIPA.createGroup(dafOrg.groupCn) )
      b <- EitherT( apiClientIPA.createUser(defaultOrgIpaUser) )
      //b <- EitherT( apiClientIPA.addUsersToGroup(dafOrg.groupCn, UserList(Option(Seq(defaultOrgIpaUser.uid)))) )
      c <- EitherT( createSuspersetDatabase(dafOrg) )
      orgAdminRoleId <- EitherT( findSupersetRoleId(ConfigReader.suspersetOrgAdminRole) )
      dataOrgRoleId <- EitherT( findSupersetRoleId(toRoleName(dafOrg.groupCn)) )
      d <- EitherT( createSupersetUserWithRoles(defaultOrgIpaUser,orgAdminRoleId,dataOrgRoleId) )
    } yield d

    result.value

    /*
    result.value.flatMap{

      case Right(r) => result.value
      case Left(l) => {
        deleteDafOrganization(dafOrg.groupCn)
        result.value
      }

    }*/

  }


  def deleteDafOrganization(groupCn:String):Future[Either[Error,Success]] = {

    val result = for {
      a <- EitherT( apiClientIPA.deleteGroup(groupCn) )
      b <- EitherT( apiClientIPA.deleteUser(toUserName(groupCn)) )

      dbId <- EitherT( findSupersetDatabaseId(toDbName(groupCn)) )
      c <- EitherT( deleteSupersetDatabase(dbId) )

      roleId <- EitherT( findSupersetRoleId((toRoleName(groupCn))) )
      d <- EitherT( deleteSupersetRole(roleId) )

      userId <- EitherT( findSupersetUserId(toUserName(groupCn)) )
      e <- EitherT( deleteSupersetUser(userId) )
    } yield e

    result.value
  }


  def addNewUserToOrganization(groupCn:String,ipaUser:IpaUser):Future[Either[Error,Success]] = {

    val result = for {
      orgAdminRoleId <- EitherT( findSupersetRoleId(ConfigReader.suspersetOrgAdminRole) )
      dataOrgRoleId <- EitherT( findSupersetRoleId(toRoleName(groupCn)) )
      a <- EitherT( createSupersetUserWithRoles(ipaUser,orgAdminRoleId,dataOrgRoleId) )
    } yield a

    result.value
  }


  private def createSuspersetDatabase(dafOrg:DafOrg): Future[Either[Error,Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val stringRequest = ConfigReader.suspersetDbUri.startsWith("impala") match{

        case true => s"""{
                          "database_name": "${toDbName(dafOrg.groupCn)}",
                          "extra":"${StringEscapeUtils.escapeJson("""{ "metadata_params": {}, "engine_params": { "connect_args": {"use_ssl":"true"}} }""")}",
                          "sqlalchemy_uri": "${ConfigReader.suspersetDbUri}/${dafOrg.supSetConnectedDbName}?auth_mechanism=PLAIN&password=${dafOrg.defaultUserPwd}&user=${toUserName(dafOrg.groupCn)}",
                          "impersonate_user": "true"
                          }"""
        // for testing pourpose
        case false =>s"""{
                          "database_name": "${toDbName(dafOrg.groupCn)}",
                          "extra":"${StringEscapeUtils.escapeJson("""{ {"metadata_params": {},"engine_params": {}}""")}",
                          "sqlalchemy_uri": "${ConfigReader.suspersetDbUri}",
                          "impersonate_user": "false"
                          }"""

      }


      val jsonRequest: JsValue = Json.parse(stringRequest)

      println("createSuspersetDatabase request: "+jsonRequest.toString())

      wSClient.url(ConfigReader.supersetUrl + "/databaseview/api/create").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map { json =>

      ((json \ "item") \ "perm").validate[String] match {
        case s:JsSuccess[String] => s.asOpt match{
          case None | Some("None") | Some("") => Left(Error(Option(0), Some("Error in createSuspersetDatabase"), None))
          case _ => Right(Success(Some("Connection created"), Some("ok")))
        }
        case e:JsError => Left(Error(Option(0), Some("Error in createSuspersetDatabase"), None))
      }

    }


  }


  //org-admin
  //datarole-${dafOrg.supSetConnectionName}
  private def findSupersetRoleId(roleName:String): Future[Either[Error,Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/read?_flt_1_name=$roleName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    println("findSupersetRoleId roleName: "+roleName)
    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{ json =>

      (json \ "pks")(0).validate[Long] match {
        case s:JsSuccess[Long] => Right(s.value)
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in findSupersetRoleId"), None))
      }

    }


  }


  private def createSupersetUserWithRoles(ipaUser:IpaUser, orgAdminRoleId:Long, dataOrgRoleId:Long ): Future[Either[Error,Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val jsonRequest: JsValue = Json.parse(s"""{
                                                "active": true,
                                                "email": "${ipaUser.mail}",
                                                "first_name": "${ipaUser.givenname}",
                                                "last_name": "${ipaUser.sn}",
                                                "username": "${ipaUser.uid}",
                                                "roles": ["$orgAdminRoleId","$dataOrgRoleId"]
                                                }""")



      println( "createSupersetUserWithRole request: "+jsonRequest.toString() )

      wSClient.url(ConfigReader.supersetUrl + "/users/api/create").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{json =>

      ((json \ "item") \ "username").validate[String] match {
        case s:JsSuccess[String] => Right(Success(Some("Connection created"), Some("ok")))
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in createSupersetUserWithRole"), None))
      }

    }

  }

  private def findSupersetDatabaseId(dbName:String): Future[Either[Error,Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/databaseview/api/read?_flt_1_database_name=$dbName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    println( "findSupersetDatabaseId dbName: "+dbName )

    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{ json =>

      (json \ "pks")(0).validate[Long] match {
        case s:JsSuccess[Long] => Right(s.value)
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in findSupersetDatabaseId"), None))
      }

    }

  }


  private def findSupersetUserId(username:String): Future[Either[Error,Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/users/api/read?_flt_1_username=$username").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    println( "findSupersetUserId username: "+username )

    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{ json =>

      (json \ "pks")(0).validate[Long] match {
        case s:JsSuccess[Long] => Right(s.value)
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in findSupersetUserId"), None))
      }

    }

  }


  private def deleteSupersetUser(userId:Long): Future[Either[Error,Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println( "deleteSupersetUser userId: "+userId )

      wSClient.url(ConfigReader.supersetUrl + s"/users/api/delete/$userId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    println( "deleteSupersetUser userId: "+userId )

    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{json =>

      (json \ "message").validate[String] match {
        case s:JsSuccess[String] => Right(Success(Some("User deleted"), Some("ok")))
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in deleteSupersetUser"), None))
      }

    }

  }

  private def deleteSupersetRole(roleId:Long): Future[Either[Error,Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println( "deleteSupersetRole roleId: "+roleId )

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/delete/$roleId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    println( "deleteSupersetRole roleId: "+roleId )

    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{json =>

      (json \ "message").validate[String] match {
        case s:JsSuccess[String] => Right(Success(Some("Role deleted"), Some("ok")))
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in deleteSupersetRole"), None))
      }

    }

  }

  private def deleteSupersetDatabase(dbId:Long): Future[Either[Error,Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println( "deleteSupersetDatabase dbId: "+dbId )

      wSClient.url(ConfigReader.supersetUrl + s"/databaseview/api/delete/$dbId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    println( "deleteSupersetDatabase dbId: "+dbId )

    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{json =>

      (json \ "message").validate[String] match {
        case s:JsSuccess[String] => Right(Success(Some("Db deleted"), Some("ok")))
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in deleteSupersetDatabase"), None))
      }

    }

  }

}
