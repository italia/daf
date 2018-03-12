package controllers

import javax.inject.Inject

import it.gov.daf.common.sso.common.{CacheWrapper, CredentialManager, LoginInfo}
import it.gov.daf.common.utils.Credentials
import it.gov.daf.securitymanager.service.utilities.RequestContext.execInContext
import it.gov.daf.sso.LoginClientLocal
import play.api.inject.ConfigurationProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}


class SSOController @Inject()(ws: WSClient, config: ConfigurationProvider, implicit val credentialManager:CredentialManager, cacheWrapper: CacheWrapper, loginClientLocal:LoginClientLocal) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val cookieWrites = Json.writes[Cookie]


  private def doWithCredentials(request:RequestHeader, f:(Credentials)=> Future[Result]):Future[Result]={

    credentialManager.getCredentials(request) match{
      case Success(crd) => f(crd)
      case Failure(thw) => Future{InternalServerError(thw.getMessage)}
    }

  }

  //----------------SECURED API---------------------------------------

  // servono le credenziali da BA
  def register = Action { implicit request =>
    execInContext[Result] { () =>

      val authHeader = request.headers.get("authorization").get.split(" ")
      val authType = authHeader(0)

      if (authType.equalsIgnoreCase("basic"))
        Ok("Success")
      else
        InternalServerError("Basic Authentication required")

    }
  }

  // serve token JWT o BA
  def retriveCookie(appName:String) = Action.async { implicit request =>
    execInContext[Future[Result]] { () =>

      def callService(crd: Credentials): Future[Result] = {

        val loginInfo = new LoginInfo(crd.username, crd.password, appName)
        loginClientLocal.loginFE(loginInfo, ws).map { cookies =>
          Ok(Json.toJson(cookies.map(Json.toJson(_))))
        }

      }

      doWithCredentials(request, callService)

    }
  }


  // serve token JWT o BA
  def retriveCachedCookie(appName:String) = Action.async { implicit request =>
    execInContext[Future[Result]] { () =>

      def callService(crd: Credentials): Future[Result] = {

        cacheWrapper.getCookies(appName, crd.username) match {

          case Some(cachedCookies) => Future {
            Ok(Json.toJson(cachedCookies.map(Json.toJson(_))))
          }

          case None => val loginInfo = new LoginInfo(crd.username, crd.password, appName)
            loginClientLocal.loginFE(loginInfo, ws).map { cookies =>
              cacheWrapper.putCookies(appName, crd.username, cookies)
              Ok(Json.toJson(cookies.map(Json.toJson(_))))
            }
        }

      }

      doWithCredentials(request, callService)

    }
  }

  // serve token JWT o BA
  def login(appName:String) = Action.async { implicit request =>
    execInContext[Future[Result]] { () =>

      def callService(crd: Credentials): Future[Result] = {

        val loginInfo = new LoginInfo(crd.username, crd.password, appName)
        loginClientLocal.loginFE(loginInfo, ws).map { cookies =>
          Ok("Success").withCookies(cookies: _*)
        }

      }

      doWithCredentials(request, callService)
    }
  }

  // serve token JWT
  def test = Action { implicit request =>
    execInContext[Result] { () =>
      val username = credentialManager.readCredentialFromRequest(request).username

      if (cacheWrapper.getPwd(username).isEmpty) {
        Unauthorized("JWT expired")
      } else
        Ok("Success")
    }
  }


  //-----------------UNSECURED API-----------------------------------------

  def registerInternal(username:String,password:String) = Action { implicit request =>
    execInContext[Result] { () =>

      if (username != null && password != null) {
        cacheWrapper.putCredentials(username, password)
        Ok("Success")
      } else
        NotAcceptable("Username and password required")

    }
  }


  def retriveCookieInternal(username:String,appName:String) = Action.async { implicit request =>
    execInContext[Future[Result]] { () =>

      val loginInfo = new LoginInfo(
        username,
        cacheWrapper.getPwd(username).get,
        appName)

      loginClientLocal.login(loginInfo, ws).map { cookie => Ok(Json.toJson(cookie)) }

    }
  }


}
