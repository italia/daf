package it.gov.daf.catalogmanager.tempo

import catalog_manager.yaml.{Error, IpaUser, Success}
import play.api.libs.json.{JsError, JsSuccess, JsValue}

import scala.concurrent.Future

object RegistrationService {

  import catalog_manager.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  private val tokenGenerator = new BearerTokenGenerator


  def requestRegistration(user:IpaUser):Either[String,MailService] = {

    if (user.userpassword.isEmpty || user.userpassword.get.length <= 5)
      Left("Password minimum length is 5 character")
    else {
      MongoService.findUserByUid(user.uid) match {
        case Right(o) => Left("Username already registered")
        case Left(o) => Right(registration(user))
      }
    }

  }


  def createUser(token:String): Future[Either[Error,Success]] = {

    MongoService.findAndRemoveUserByToken(token) match{
      case Right(json) => createUser(json)
      case Left(l) => Future{ Left( Error(None,Some("User pre-registration not found"),None) )}
    }

  }

  private def createUser(json:JsValue):Future[Either[Error,Success]] = {

    val result = json.validate[IpaUser]
    result match {
      case s: JsSuccess[IpaUser] =>  ApiClientIPA.createUser(s.get)
      case e: JsError => Future{ Left( Error(None,Some("Error during user data conversion"),None) )}
    }

  }


  private def registration(user:IpaUser):MailService = {

    val token = tokenGenerator.generateMD5Token(user.uid)

    MongoService.writeUserData(user,token)

    new MailService(user.mail,token)

  }

}
