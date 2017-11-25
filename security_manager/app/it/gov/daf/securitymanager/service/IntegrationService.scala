package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.sso.ApiClientIPA
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.WSResponse
import security_manager.yaml.{DafOrg, Error, IpaUser, Success}

import scala.concurrent.Future
import cats.implicits._
import org.apache.commons.lang3.StringEscapeUtils

@Singleton
class IntegrationService @Inject()(apiClientIPA:ApiClientIPA,secInvokeManager:SecuredInvocationManager){

  import scala.concurrent.ExecutionContext.Implicits._

  private val loginInfoSuperset = new LoginInfo(ConfigReader.suspersetAdminUser,ConfigReader.suspersetAdminPwd,"superset")

  def createDafOrganization(dafOrg:DafOrg):Future[Either[Error,Success]] = {


    val defaultOrgIpaUser = new IpaUser(  dafOrg.groupCn,
                                          "default org admin",
                                          s"${dafOrg.groupCn}@default.it",
                                          s"${dafOrg.groupCn}-default-admin",
                                          Option(Role.Admin.toString),
                                          Option(dafOrg.defaultUserPwd),
                                          Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- EitherT( apiClientIPA.createGroup(dafOrg.groupCn) )
      b <- EitherT( apiClientIPA.createUser(defaultOrgIpaUser) )
      c <- EitherT( createSuspersetDatabase(dafOrg) )
      orgAdminRoleId <- EitherT( findSupersetRoleId(ConfigReader.suspersetOrgAdminRole) )
      dataOrgRoleId <- EitherT( findSupersetRoleId(s"datarole-${dafOrg.supSetConnectionName}") )
      d <- EitherT( createSupersetUserWithRoles(defaultOrgIpaUser,orgAdminRoleId,dataOrgRoleId) )
    } yield d

    result.value
  }


  private def createSuspersetDatabase(dafOrg:DafOrg): Future[Either[Error,Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: AhcWSClient): Future[WSResponse] = {

      val jsonRequest: JsValue = Json.parse(s"""{
                                              "database_name": "${dafOrg.supSetConnectionName}",
                                              "extra":"${StringEscapeUtils.escapeJson("""{"metadata_params": {},"engine_params": {}}""")}",
                                              "sqlalchemy_uri": "${dafOrg.supSetConnectionString}",
                                              "impersonate_user": "false"
                                              }""")

      println("createSuspersetDatabase request: "+jsonRequest.toString())

      wSClient.url(ConfigReader.supersetUrl + "/databaseview/api/create").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map { json =>

      ((json \ "item") \ "perm").validate[String] match {
        case s:JsSuccess[String] => Right(Success(Some("Connection created"), Some("ok")))
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in createSuspersetDatabase"), None))
      }

    }


  }


  //org-admin
  //datarole-${dafOrg.supSetConnectionName}
  private def findSupersetRoleId(roleName:String): Future[Either[Error,Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: AhcWSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/read?_flt_1_name=$roleName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    secInvokeManager.manageServiceCall(loginInfoSuperset,serviceInvoke).map{ json =>

      (json \ "pks")(0).validate[Long] match {
        case s:JsSuccess[Long] => Right(s.value)
        case e:JsError => println("Error response: "+json);Left(Error(Option(0), Some("Error in findSupersetRoleId"), None))
      }

    }


  }


  private def createSupersetUserWithRoles(ipaUser:IpaUser, orgAdminRoleId:Long, dataOrgRoleId:Long ): Future[Either[Error,Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: AhcWSClient): Future[WSResponse] = {

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


}
