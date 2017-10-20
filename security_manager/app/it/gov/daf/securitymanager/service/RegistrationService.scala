package it.gov.daf.securitymanager.service

import it.gov.daf.securitymanager.service.utilities.BearerTokenGenerator
import it.gov.daf.sso.ApiClientIPA
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import security_manager.yaml.IpaUser
import security_manager.yaml.Success
import security_manager.yaml.Error

import scala.concurrent.Future

object RegistrationService {

  import security_manager.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  private val tokenGenerator = new BearerTokenGenerator


  def requestRegistration(user:IpaUser):Future[Either[String,MailService]] = {

    if (user.userpassword.isEmpty || user.userpassword.get.length < 8)
      Future{Left("Password minimum length is 8 characters")}
    else if( !user.userpassword.get.matches("^[a-zA-Z0-9%@#   &,;:_'/\\\\<\\\\(\\\\[\\\\{\\\\\\\\\\\\^\\\\-\\\\=\\\\$\\\\!\\\\|\\\\]\\\\}\\\\)\u200C\u200B\\\\?\\\\*\\\\+\\\\.\\\\>]*$") )
      Future{Left("Invalid chars in password")}
    else{
      MongoService.findUserByUid(user.uid) match {
        case Right(o) => Future{Left("Username already requested")}
        case Left(o) => checkUserNregister(user)
      }
    }

  }

  private def checkUserNregister(user:IpaUser):Future[Either[String,MailService]] = {

    ApiClientIPA.showUser(user.uid) flatMap { result =>

      result match{
        case Right(r) => Future { Left("Username already registered") }
        case Left(l) => checkMailNregister(user)
      }

    }

  }

  private def checkMailNregister(user:IpaUser):Future[Either[String,MailService]] = {

    ApiClientIPA.findUserByMail(user.mail) flatMap { result =>

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

    ApiClientIPA.showUser(user.uid) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Username already registered"), None))}
        case Left(l) => checkMailNcreateUser(user)
      }

    }
  }


  private def checkMailNcreateUser(user:IpaUser):Future[Either[Error,Success]] = {

    ApiClientIPA.findUserByMail(user.mail) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Mail address already registered"), None))}
        case Left(l) => ApiClientIPA.createUser(user)
      }

    }
  }


}