package it.gov.daf.sso.client

import it.gov.daf.sso.common.LoginInfo
import it.gov.daf.sso.common.LoginClient
import play.api.libs.ws.WSClient

import scala.concurrent.Future


class LoginClientRemote extends LoginClient{

  def login(loginInfo:LoginInfo, client: WSClient):Future[String] = {

    if(client != null )
      SsoServiceClientBase.retriveCookieInternal(loginInfo.user,loginInfo.appName,client)
    else
      SsoServiceClient.retriveCookieInternal(loginInfo.user,loginInfo.appName)

  }

}


object LoginClientRemote {
  private val _instance = new LoginClientRemote()
  def instance() = _instance
}



