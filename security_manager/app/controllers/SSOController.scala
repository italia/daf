package controllers

import javax.inject.Inject

import it.gov.daf.common.authentication.Authentication
import it.gov.daf.common.sso.common.{CacheWrapper, CredentialManager, LoginInfo}
import it.gov.daf.common.utils.Credentials
import it.gov.daf.common.utils.RequestContext.execInContext
import it.gov.daf.securitymanager.utilities.{ConfigReader, Utils}
import it.gov.daf.sso.LoginClientLocal
import play.api.Logger
import play.api.inject.ConfigurationProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}


class SSOController @Inject()(ws: WSClient, config: ConfigurationProvider, cacheWrapper: CacheWrapper, loginClientLocal:LoginClientLocal) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val cookieWrites = Json.writes[Cookie]

  private val logger = Logger(this.getClass.getName)


  private def doWithCredentials(request:RequestHeader, f:(Credentials)=> Future[Result]):Future[Result]={

    Utils.getCredentials(request,cacheWrapper) match{
      case Success(crd) => f(crd)
      case Failure(thw) => Future{InternalServerError(thw.getMessage)}
    }

  }

  //----------------SECURED API---------------------------------------

  // servono le credenziali da BA
  def register = Action { implicit request =>
    execInContext[Result] ("register"){ () =>

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
    execInContext[Future[Result]] ("retriveCookie"){ () =>

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
    execInContext[Future[Result]] ("retriveCachedCookie") { () =>

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
    execInContext[Future[Result]] ("login"){ () =>

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
    execInContext[Result] ("test") { () =>
      val username = CredentialManager.readCredentialFromRequest(request).username

      if (cacheWrapper.getPwd(username).isEmpty) {
        Unauthorized("JWT expired")
      } else
        Ok("Success")
    }
  }

  def biOpenLogin  = Action.async (parse.urlFormEncoded){ implicit request =>
    execInContext[Future[Result]] ("biOpenLogin"){ () =>

      val token:Option[String] = request.body("Authorization").headOption.map(str => str.substring(7,str.length))
      val loginInfo = new LoginInfo(ConfigReader.suspersetOpenDataUser, ConfigReader.suspersetOpenDataPwd, LoginClientLocal.SUPERSET_OPEN)

      logger.debug(s"token $token")

      def performSupersetOpenDataLogin = loginClientLocal.loginFE(loginInfo, ws) map { cookies =>

        logger.debug("performSupersetOpenDataLogin")

        scala.util.Try {
          val openDataDomain = {  val openDataUrl = ConfigReader.supersetOpenUrl
                                  openDataUrl.substring(openDataUrl.indexOf('.'),openDataUrl.length) }

          logger.debug(s"setting cookie on domain: $openDataDomain")

          val biCookie = cookies.head.copy(domain = Option(openDataDomain))
          val content = views.html.Application.bi_open_login(biCookie)
          Ok(content)
        }match {
          case Success(v) => v
          case Failure(e) => logger.error("Stack trace:",e); InternalServerError(s"Problems while getting cookie: ${e.getMessage}")
        }

      }

      scala.util.Try{
        Authentication.getClaimsFromToken(token)
      }match {
        case Success(v) => performSupersetOpenDataLogin
        case Failure(e) => logger.error("Stack trace:",e); Future.successful(Unauthorized(s"Authentication issue"))
      }

      /*
      val out = for{
        a <- ws.url("http://security-manager.default.svc.cluster.local:9000/security-manager/v1/token").withHeaders("Authorization" -> bearer).get
        b <- if(a.status == 200) loginClientLocal.loginFE(loginInfo, ws) else{ logger.error("resp status:"+a.status+"   body:"+a.body);Future.successful{Seq.empty[Cookie]} }
      } yield b


      out.map { cookies =>

        logger.debug("mapping")

        val tt = scala.util.Try {
          val biCookie = cookies.head.copy(domain = Option(".open.daf.teamdigitale.test"))
          val content = views.html.Application.bi_open_login(biCookie)
          Ok(content)
        }

        tt match {
          case Success(v) => v
          case Failure(e) => logger.error("Stack trace:",e); Unauthorized(s"Problems while getting cookie: ${e.getMessage}")
        }

      }*/

    }
  }

  /*
  def biOpenLoginH  = Action.async { implicit request =>
    execInContext[Future[Result]] ("biOpenLoginH"){ () =>

      //val bearer:String = request.body("Authorization").head
      val loginInfo = new LoginInfo("new_andrea", "Password1", LoginClientLocal.SUPERSET)

      loginClientLocal.loginFE(loginInfo, ws).map { cookies =>

        val biCookie = cookies.head.copy( domain = Option(".open.daf.teamdigitale.test"))
        val content = views.html.Application.bi_open_login(biCookie)
        Ok(content)

      }

    }
  }*/


  //-----------------UNSECURED API-----------------------------------------
/*
  def registerInternal(username:String,password:String) = Action { implicit request =>
    execInContext[Result] { () =>

      if (username != null && password != null) {
        cacheWrapper.putCredentials(username, password)
        Ok("Success")
      } else
        NotAcceptable("Username and password required")

    }
  }
*/

  def retriveAdminCookieInternal(appName:String) = Action.async { implicit request =>
    execInContext[Future[Result]] ("retriveAdminCookieInternal"){ () =>
      loginClientLocal.loginAdmin(appName, ws).map { cookie => Ok(Json.toJson(cookie)) }
    }
  }


  def retriveCookieInternal(username:String,appName:String) = Action.async { implicit request =>
    execInContext[Future[Result]] ("retriveCookieInternal"){ () =>

      val loginInfo = new LoginInfo(
        username,
        cacheWrapper.getPwd(username).get,
        appName)

      loginClientLocal.login(loginInfo, ws).map { cookie => Ok(Json.toJson(cookie)) }

    }
  }


}
