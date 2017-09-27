package controllers

import javax.inject.Inject

import it.gov.daf.securitymanager.service.LoginClient
import it.gov.daf.securitymanager.service.utilities.WebServiceUtil
import play.api.cache.CacheApi
import play.api.inject.ConfigurationProvider
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller, Cookie}

import scala.concurrent.duration._


class SSOController @Inject()(ws: WSClient, cache: CacheApi, config: ConfigurationProvider) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  private val CACHE_TTL = 60.minutes // TODO TTL da impostare come token jwt

  //----------------SECURED API---------------------------------------

  // richiede credenziali da BA
  def register = Action { implicit request =>

    val credentials = WebServiceUtil.readCredentialFromRequest(request)

    if( !credentials._2.isEmpty ) {
      cache.set(credentials._1.get, credentials._2.get, CACHE_TTL)
      Ok("Success")
    }else
      InternalServerError("Basic Authentication required")

  }


  // richiede token JWT o BA
  def retriveCookie(appName:String) = Action.async { implicit request =>

    val username = WebServiceUtil.readCredentialFromRequest(request)._1.get
    val pwd = cache.get[String](username).get

    LoginClient.login(username, pwd, ws, appName).map{ cookie =>
      Ok(cookie)
    }

  }


  // richiede token JWT o BA
  def login(username:String, appName:String) = Action.async { implicit request =>

    val username = WebServiceUtil.readCredentialFromRequest(request)._1.get
    val pwd = cache.get[String](username).get

    LoginClient.login(username, pwd, ws, appName).map{ cookie =>
      val cookieDetail = cookie.split("=")
      Ok("Success").withCookies( Cookie(cookieDetail(0),cookieDetail(1)) )
    }

  }


  //-----------------UNSECURED API-----------------------------------------

  def registerInternal(username:String,password:String) = Action { implicit request =>

    if( username!=null &&  password != null) {
      cache.set(username, password, CACHE_TTL)
      Ok("Success")
    }else
      NotAcceptable("Username and password required")

  }


  def retriveCookieInternal(username:String,appName:String) = Action.async { implicit request =>

    val pwd = cache.get[String](username).get
    LoginClient.login(username, pwd, ws, appName).map{ cookie => Ok(cookie) }

  }


}
