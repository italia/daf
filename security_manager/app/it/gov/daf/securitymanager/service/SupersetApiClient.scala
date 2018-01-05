package it.gov.daf.securitymanager.service

import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, IpaUser, Success}
import cats.implicits._
import org.apache.commons.lang3.StringEscapeUtils
import scala.concurrent.Future

@Singleton
class SupersetApiClient @Inject()(secInvokeManager: SecuredInvocationManager){

  import scala.concurrent.ExecutionContext.Implicits._

  private val loginAdminSuperset = new LoginInfo(ConfigReader.suspersetAdminUser, ConfigReader.suspersetAdminPwd, "superset")


  def createDatabase(dataSource:String, userName:String, userPwd:String, connectedDbName:String ): Future[Either[Error, Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val stringRequest = ConfigReader.suspersetDbUri.startsWith("impala") match {

        case true => s"""{
                          "database_name": "$dataSource",
                          "extra":"${StringEscapeUtils.escapeJson("""{ "metadata_params": {}, "engine_params": { "connect_args": {"use_ssl":"true"}} }""")}",
                          "sqlalchemy_uri": "${ConfigReader.suspersetDbUri}/$connectedDbName?auth_mechanism=PLAIN&password=$userPwd&user=$userName",
                          "impersonate_user": "false"
                          }"""
        // for testing pourpose
        case false => s"""{
                          "database_name": "$dataSource",
                          "extra":"${StringEscapeUtils.escapeJson("""{ "metadata_params": {},"engine_params": {} }""")}",
                          "sqlalchemy_uri": "${ConfigReader.suspersetDbUri}",
                          "impersonate_user": "false"
                          }"""

      }


      val jsonRequest: JsValue = Json.parse(stringRequest)

      println("createSuspersetDatabase request: " + jsonRequest.toString())

      wSClient.url(ConfigReader.supersetUrl + "/databaseview/api/create").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      ((json \ "item") \ "perm").validate[String] match {
        case s: JsSuccess[String] => s.asOpt match {
          case None | Some("None") | Some("") => Left(Error(Option(0), Some("Error in createSuspersetDatabase"), None))
          case _ => Right(Success(Some("Connection created"), Some("ok")))
        }
        case e: JsError => Left(Error(Option(0), Some("Error in createSuspersetDatabase"), None))
      }

    }


  }


  def findRoleIds(roleNames: String*): Future[Either[Error, List[Long]]] = {

    val traversed = roleNames.toList.traverse[Future, Either[Error, Long]](findRoleId): Future[List[Either[Error, Long]]]

    traversed.map { lista =>
      val out = lista.foldLeft(List[Long]())((a, b) => b match {
        case Right(r) => (r :: a)
        case _ => a
      })
      Right(out)
    }

  }


  def findRoleId(roleName: String): Future[Either[Error, Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/read?_flt_1_name=$roleName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    println("findSupersetRoleId roleName: " + roleName)
    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      (json \ "pks") (0).validate[Long] match {
        case s: JsSuccess[Long] => Right(s.value)
        case e: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in findSupersetRoleId"), None))
      }

    }


  }


  def createUserWithRoles(ipaUser: IpaUser, roleIds: Long*): Future[Either[Error, Success]] = {

    val roleIdsJsonString = roleIds.mkString("[\"", "\",\"", "\"]")

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val jsonRequest: JsValue = Json.parse(
        s"""{
                                                "active": true,
                                                "email": "${ipaUser.mail}",
                                                "first_name": "${ipaUser.givenname}",
                                                "last_name": "${ipaUser.sn}",
                                                "username": "${ipaUser.uid}",
                                                "roles": $roleIdsJsonString
                                                }""")


      println("createSupersetUserWithRole request: " + jsonRequest.toString())

      wSClient.url(ConfigReader.supersetUrl + "/users/api/create").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      ((json \ "item") \ "username").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("Connection created"), Some("ok")))
        case e: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in createSupersetUserWithRole"), None))
      }

    }

  }


  def findDatabaseId(dbName: String): Future[Either[Error, Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/databaseview/api/read?_flt_1_database_name=$dbName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    println("findSupersetDatabaseId dbName: " + dbName)

    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      (json \ "pks") (0).validate[Long] match {
        case s: JsSuccess[Long] => Right(s.value)
        case e: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in findSupersetDatabaseId"), None))
      }

    }

  }


  type SupersetUserInfo = (Long, Array[String])

  def findUser(username: String): Future[Either[Error, SupersetUserInfo]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/users/api/read?_flt_1_username=$username").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    println("findSupersetUserId username: " + username)

    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      (json \ "pks") (0).validate[Long] match {
        case s: JsSuccess[Long] => ((json \ "result") (0) \ "roles").validate[Array[String]] match {
          case s2: JsSuccess[Array[String]] => Right((s.value, s2.value))
          case e2: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in findSupersetUserId"), None))
        }
        case e: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in findSupersetUserId"), None))
      }

    }

  }


  def deleteUser(userId: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println("deleteSupersetUser userId: " + userId)

      wSClient.url(ConfigReader.supersetUrl + s"/users/api/delete/$userId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    println("deleteSupersetUser userId: " + userId)

    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("User deleted"), Some("ok")))
        case e: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in deleteSupersetUser"), None))
      }

    }

  }


  def deleteRole(roleId: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println("deleteSupersetRole roleId: " + roleId)

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/delete/$roleId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    println("deleteSupersetRole roleId: " + roleId)

    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("Role deleted"), Some("ok")))
        case e: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in deleteSupersetRole"), None))
      }

    }

  }

  def deleteDatabase(dbId: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println("deleteSupersetDatabase dbId: " + dbId)

      wSClient.url(ConfigReader.supersetUrl + s"/databaseview/api/delete/$dbId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    println("deleteSupersetDatabase dbId: " + dbId)

    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("Db deleted"), Some("ok")))
        case e: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in deleteSupersetDatabase"), None))
      }

    }

  }



  def checkDbTables(dbId:Long):Future[Either[Error,Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/tablemodelview/api/readvalues?_flt_0_database=$dbId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    println("checkDbTables id: " + dbId)

    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map { json =>

      json(0).validate[JsValue] match {
        case s: JsSuccess[JsValue] =>  Left(Error(Option(0), Some("Some tables on Superset founded. Please delete them before cancel datasource"), None))
        case e: JsError =>  Right(Success(Some("Tables not presents"), Some("ok")))
      }

    }

  }



}
