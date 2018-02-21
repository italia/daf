package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, Success}
import cats.implicits._
import it.gov.daf.sso.LoginClientLocal
import scala.concurrent.Future

@Singleton
class GrafanaApiClient @Inject()(secInvokeManager: SecuredInvocationManager,loginClientLocal: LoginClientLocal,wSClient: WSClient){

  import play.api.libs.concurrent.Execution.Implicits._

  private val loginAdminGrafana = new LoginInfo(ConfigReader.grafanaAdminUser, ConfigReader.grafanaAdminPwd, "grafana")
  //private val grafanaAuthUrl = createGrafanaAuthUrl()

/*
  private def createGrafanaAuthUrl():String ={
    val grafanaModUrl = new StringBuilder(ConfigReader.grafanaUrl)
    grafanaModUrl.insert(ConfigReader.grafanaUrl.indexOf("""//""")+2 ,s"${ConfigReader.grafanaAdminUser}:${ConfigReader.grafanaAdminPwd}@")
    grafanaModUrl.toString()
  }*/

  private def handleServiceCall[A](serviceInvoke:(String,WSClient)=> Future[WSResponse], handleJson:(JsValue)=> Either[Error, A] )={

    secInvokeManager.manageRestServiceCall(loginAdminGrafana, serviceInvoke,200).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }
  }

  def createOrganization(orgName:String): Future[Either[Error, Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val stringRequest = s"""{
                          "name": "$orgName"
                          }"""

      val jsonRequest: JsValue = Json.parse(stringRequest)

      println("create grafana organization request: " + jsonRequest.toString())

      wSClient.url(ConfigReader.grafanaUrl+ "/api/orgs").withHeaders( "Content-Type" -> "application/json",
                                                                      "Accept" -> "application/json",
                                                                      "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    def handleJson(json:JsValue)= {

      (json \ "orgId").validate[Long] match {
        case s: JsSuccess[Long] => Right(Success(Some("Organization created"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in create grafana organization"), None))
      }

    }

    handleServiceCall(serviceInvoke,handleJson)

  }


  def logNewUser(username:String, pwd:String): Future[Either[Error, Success]] = {

    println("logNewUser:"+username)
    loginClientLocal.login(new LoginInfo(username,pwd,"grafana"),wSClient).map{ _ => Right(Success(Some("Logged"), Some("ok"))) }

  }


  def addUserInOrganization(orgName:String, username:String): Future[Either[Error, Success]] = {

    val result = for {
      orgId <- EitherT( getOrganizationId(orgName) )
      a <- EitherT( addUserInOrganization(orgId,username) )
    } yield a

    result.value

  }

  def addNewUserInOrganization(orgName:String, username:String, pwd:String): Future[Either[Error, Success]] = {

    val result = for {
      orgId <- EitherT( getOrganizationId(orgName) )
      a <- EitherT( logNewUser(username,pwd) )// login for user creation
      b <- EitherT( addUserInOrganization(orgId,username) )
    } yield b

    result.value

  }

  def deleteOrganization(orgName:String): Future[Either[Error, Success]] = {

    val result = for {
      orgId <- EitherT( getOrganizationId(orgName) )
      a <- EitherT( deleteOrganization(orgId) )
    } yield a

    result.value

  }


  private def deleteOrganization(orgId:Long): Future[Either[Error, Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println("grafana deleteOrganization orgId: "+orgId)

      wSClient.url(ConfigReader.grafanaUrl+ s"/api/orgs/$orgId").withHeaders( "Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }



    def handleJson(json:JsValue)= {

      (json \ "message").validate[String] match {

        case s: JsSuccess[String] => s.asOpt match {
          case Some(i) => if (i.contains("Organization deleted"))
            Right(Success(Some("Organization deleted"), Some("ok")))
          else
            Left(Error(Option(0), Some("Error in grafana deleteOrganization"), None))

          case None => Left(Error(Option(0), Some("Error in grafana deleteOrganization"), None))
        }

        case e: JsError => Left(Error(Option(0), Some("Error in grafana deleteOrganization"), None))
      }

    }

    handleServiceCall(serviceInvoke,handleJson)

  }




  private def getOrganizationId(orgName:String): Future[Either[Error, Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      println(" grafana getOrganizationId orgName: "+orgName)

      println("sessionCookie:"+sessionCookie)

      wSClient.url(ConfigReader.grafanaUrl+ s"/api/orgs/name/$orgName").withHeaders( "Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie,
        "Cookie" -> s"grafana_user=${ConfigReader.grafanaAdminUser}"
      ).get()
    }


    def handleJson(json:JsValue)= {

      (json \ "id").validate[Long] match {
        case s: JsSuccess[Long] => s.asOpt match {
          case Some(i) => Right(i)
          case None => Left(Error(Option(0), Some("Error in grafana getOrganizationId"), None))
        }
        case e: JsError => Left(Error(Option(0), Some("Error in grafana getOrganizationId"), None))
      }

    }

    handleServiceCall(serviceInvoke,handleJson)

  }


  private def addUserInOrganization(orgId:Long, username:String): Future[Either[Error, Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val stringRequest = s"""{
                            "loginOrEmail":"$username",
                            "role":"Admin"
                          }"""

      val jsonRequest: JsValue = Json.parse(stringRequest)

      println("grafana addUserInOrganization request: " + jsonRequest.toString())

      wSClient.url(ConfigReader.grafanaUrl+ s"/api/orgs/$orgId/users").withHeaders( "Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    def handleJson(json:JsValue)= {

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => s.asOpt match {
          case Some(i) => if( i.contains("User added") )
            Right(Success(Some("User added"), Some("ok")))
          else
            Left(Error(Option(0), Some("Error in grafana getOrganizationId"), None))

          case None => Left(Error(Option(0), Some("Error in grafana getOrganizationId"), None))
        }
        case e: JsError => Left(Error(Option(0), Some("Error in grafana addUserInOrganization"), None))
      }

    }

    handleServiceCall(serviceInvoke,handleJson)


  }


}
