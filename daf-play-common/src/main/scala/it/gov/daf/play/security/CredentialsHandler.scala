package it.gov.daf.play.security

import java.util.Base64

import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc.Request

import scala.util.{Failure, Success, Try}

object CredentialsHandler {

  sealed trait DafCredentials
  case class BasicCredentials(username: String, pwd: String) extends DafCredentials
  case class JwtCredentials(username: String) extends DafCredentials
  case class UnauthorizedUser(throwable: Throwable) extends DafCredentials

  def apply(playSessionStore: PlaySessionStore, conf: Configuration): CredentialsHandler =
    new CredentialsHandler(playSessionStore, conf)
}

class CredentialsHandler(playSessionStore: PlaySessionStore, conf: Configuration) {
  import CredentialsHandler._
  private val secret: String = conf.getString("pac4j.jwt_secret").get
  require(secret.nonEmpty, "please specify a valid value for pac4j.jwt_secret")

  private val jwtAuthenticator = new JwtAuthenticator()
  jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(secret))

  def extract[T](req: Request[T]): DafCredentials = {
    req.headers.get("Authorization") match {
      case Some(value) =>
        value.split(" ").toList match {
          case "Basic" :: rawCredentials :: tail => getBasicCredentials(req, rawCredentials)
          case "Bearer" :: rawToken :: tail => getJwtCredentials(rawToken)
          case other =>
            UnauthorizedUser(new IllegalArgumentException("No Valid authentication found"))
        }

      case None =>
        UnauthorizedUser(new IllegalArgumentException("No Authorization header found"))
    }
  }

  type Username= String
  def extractUsername[T](req: Request[T]): Try[Username] = extract(req) match {
    case BasicCredentials(username, _) => Success(username)
    case JwtCredentials(username) => Success(username)
    case UnauthorizedUser(ex) => Failure(ex)
  }

  private[security] def getJwtCredentials(rawToken: String): DafCredentials = {
    Option(jwtAuthenticator.validateToken(rawToken)) match {
      case Some(profile) => JwtCredentials(profile.getUsername)
      case None => UnauthorizedUser(new IllegalArgumentException("Invalid Jwt Token"))
    }
  }

  private[security] def getBasicCredentials[T](req: Request[T], rawCredentials: String): DafCredentials = {
    getUserProfile(req) match {
      case Some(profile) =>
        //decode username and password from the raw credentials
        new String(Base64.getDecoder.decode(rawCredentials)).split(":").toList match {
          case user :: pwd :: Nil =>
            BasicCredentials(profile.getUsername, pwd)

          case other =>
            UnauthorizedUser(
              new IllegalArgumentException("No Username and Password found in Authorization header")
            )
        }

      case None =>
        UnauthorizedUser(new IllegalArgumentException("No valid profile found in Session Store"))
    }
  }

  /**
   *
   * @param req
   * @tparam T
   * @return a common profile as defined in <a href="https://github.com/pac4j/play-pac4j/tree/3.0.x#5-get-the-user-profile-profilemanager">pplay-pac4j</a>
   */
  private[security] def getUserProfile[T](req: Request[T]): Option[CommonProfile] = {
    val webContext = new PlayWebContext(req, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    import scala.compat.java8.OptionConverters._
    profileManager.get(true).asScala
  }

}
