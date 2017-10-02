package it.gov.daf.sso.common

import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future


abstract class LoginClient {

  def login(loginInfo:LoginInfo, wsClient: WSClient):Future[String]

}
