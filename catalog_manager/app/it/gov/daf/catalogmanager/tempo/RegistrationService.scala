package it.gov.daf.catalogmanager.tempo

import catalog_manager.yaml.{Error, IpaUser, Success}
import play.api.libs.json.{JsError, JsSuccess, JsValue}

import scala.concurrent.Future

object RegistrationService {

  import catalog_manager.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  val tokenGenerator = new BearerTokenGenerator

  def requestRegistration(user:IpaUser):Either[String,MailService] = {

    MongoService.findUserByUid(user.uid.get) match{
      case Right(o) => Left("Username already registered")
      case Left(o) => Right(registration(user))
    }

  }

  def createUser(token:String): Future[Either[Error,Success]] = {

    MongoService.findUserByToken(token) match{
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

    val token = tokenGenerator.generateMD5Token(user.uid.get)

    MongoService.writeUserData(user,token)

    new MailService(user.mail.get,token)

  }

}
