package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.securitymanager.service.utilities.{BearerTokenGenerator, ConfigReader}
import it.gov.daf.sso.ApiClientIPA
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import security_manager.yaml.{Error, IpaUser, Success, UserList}
import it.gov.daf.common.authentication.Role
import cats.implicits._

import scala.concurrent.Future

@Singleton
class RegistrationService @Inject()(apiClientIPA:ApiClientIPA, supersetApiClient: SupersetApiClient, grafanaApiClient: GrafanaApiClient) {

  import security_manager.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  private val tokenGenerator = new BearerTokenGenerator


  def requestRegistration(userIn:IpaUser):Future[Either[String,MailService]] = {

    val user = setUid(userIn)

    if (user.userpassword.isEmpty || user.userpassword.get.length < 8)
      Future{Left("Password minimum length is 8 characters")}
    else if( !user.userpassword.get.matches("^[a-zA-Z0-9%@#   &,;:_'/\\\\<\\\\(\\\\[\\\\{\\\\\\\\\\\\^\\\\-\\\\=\\\\$\\\\!\\\\|\\\\]\\\\}\\\\)\u200C\u200B\\\\?\\\\*\\\\+\\\\.\\\\>]*$") )
      Future{Left("Invalid chars in password")}
    else if( !user.uid.matches("^[a-zA-Z0-9_\\\\-]*$") )
      if( userIn.uid == null || userIn.uid.isEmpty )
        Future{Left("Invalid chars in mail")}
      else
        Future{Left("Invalid chars in username")}
    else{
      MongoService.findUserByUid(user.uid) match {
        case Right(o) => Future{Left("Username already requested")}
        case Left(o) => checkUserNregister(user)
      }
    }

  }


  private def setUid(user: IpaUser): IpaUser = {

    println("uid-->"+user.uid)
    if (user.uid == null || user.uid.isEmpty ) user.copy(uid = user.mail.replaceAll("[@]", "_").replaceAll("[.]", "-"))
    else user
  }

  private def checkUserNregister(user:IpaUser):Future[Either[String,MailService]] = {

    apiClientIPA.showUser(user.uid) flatMap { result =>

      result match{
        case Right(r) => Future { Left("Username already registered") }
        case Left(l) => checkMailNregister(user)
      }

    }

  }

  private def checkMailNregister(user:IpaUser):Future[Either[String,MailService]] = {

    apiClientIPA.findUserByMail(user.mail) flatMap { result =>

      result match{
        case Right(r) => Future { Left("Mail already registered") }
        case Left(l) => Future { Right(registration(user)) }
      }

    }

  }


  private def registration(user:IpaUser):MailService = {

    val token = tokenGenerator.generateMD5Token(user.uid)

    MongoService.writeUserData(user,token)

    new MailService(user.mail,token)

  }


  def createUser(token:String): Future[Either[Error,Success]] = {

    MongoService.findAndRemoveUserByToken(token) match{
      case Right(json) => checkNcreateUser(json)
      case Left(l) => Future{ Left( Error(Option(1),Some("User pre-registration not found"),None) )}
    }

  }

  private def checkNcreateUser(json:JsValue):Future[Either[Error,Success]] = {

    val result = json.validate[IpaUser]
    result match {
      case s: JsSuccess[IpaUser] =>  checkMailUidNcreateUser(s.get)
      case e: JsError => Future{ Left( Error(Option(0),Some("Error during user data conversion"),None) )}
    }

  }

  private def checkMailUidNcreateUser(user:IpaUser):Future[Either[Error,Success]] = {

    apiClientIPA.showUser(user.uid) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Username already registered"), None))}
        case Left(l) => checkMailNcreateUser(user)
      }

    }
  }


  private def checkMailNcreateUser(user:IpaUser):Future[Either[Error,Success]] = {

    apiClientIPA.findUserByMail(user.mail) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Mail address already registered"), None))}
        case Left(l) =>  createUser(user)
      }

    }
  }


  def createUser(user:IpaUser):Future[Either[Error,Success]] = {

    val userId = UserList(Option(Seq(user.uid)))

    val result = for {
      a <- EitherT( apiClientIPA.createUser(user) )
      b <- EitherT( apiClientIPA.addUsersToGroup(Role.Viewer.toString,userId) )
      c <- EitherT( addNewUserToDefultOrganization(user) )
    } yield c

    result.value

  }


  def createDefaultUser(user:IpaUser):Future[Either[Error,Success]] = {

    val userId = UserList(Option(Seq(user.uid)))

    val result = for {
      a <- EitherT( apiClientIPA.createUser(user) )
      b <- EitherT( apiClientIPA.addUsersToGroup(Role.Viewer.toString,userId) )
      c <- EitherT( addDefaultUserToDefultOrganization(user) )
    } yield c

    result.value

  }


  def addNewUserToDefultOrganization(ipaUser:IpaUser):Future[Either[Error,Success]] = {

    require(ipaUser.userpassword.nonEmpty,"user password needed!")

    val result = for {
      roleIds <- EitherT( supersetApiClient.findRoleIds(ConfigReader.suspersetOrgAdminRole,IntegrationService.toRoleName(ConfigReader.defaultOrganization)) )
      a <- EitherT( supersetApiClient.createUserWithRoles(ipaUser,roleIds:_*) )
      //b <- EitherT( grafanaApiClient.addNewUserInOrganization(ConfigReader.defaultOrganization,ipaUser.uid,ipaUser.userpassword.get) ) TODO da riattivare
    } yield a

    result.value
  }


  def addDefaultUserToDefultOrganization(ipaUser:IpaUser):Future[Either[Error,Success]] = {

    require(ipaUser.userpassword.nonEmpty,"user password needed!")

    val result = for {
      roleIds <- EitherT( supersetApiClient.findRoleIds(ConfigReader.suspersetOrgAdminRole,IntegrationService.toRoleName(ConfigReader.defaultOrganization)) )
      a <- EitherT( supersetApiClient.createUserWithRoles(ipaUser,roleIds:_*) )
      //b <- EitherT( grafanaApiClient.addNewUserInOrganization(ConfigReader.defaultOrganization,ipaUser.uid,ipaUser.userpassword.get) )
    } yield a

    result.value
  }



}