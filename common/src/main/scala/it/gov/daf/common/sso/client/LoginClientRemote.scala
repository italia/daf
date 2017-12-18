/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.gov.daf.common.sso.client


import com.google.inject.Singleton
import it.gov.daf.common.sso.common.{LoginClient, LoginInfo}
import play.api.libs.ws.WSClient
import play.api.mvc.Cookie

import scala.concurrent.Future

@Singleton
class LoginClientRemote(secManagerHost:String) extends LoginClient{

  //val secManagerHost:String = _secManagerHost

  def login(loginInfo:LoginInfo, client: WSClient):Future[Cookie] = {

    require(secManagerHost!=null, "Security Manager host must be provided")

    //if(client != null )
      SsoServiceClient.retriveCookieInternal(secManagerHost,loginInfo.user,loginInfo.appName,client)
    //else
      //SsoServiceClient.retriveCookieInternal(secManagerHost,loginInfo.user,loginInfo.appName)

  }


}
/*
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Null"
  )
)
object LoginClientRemote {

  private var _instance : LoginClientRemote = null

  def init(secManagerHost:String):LoginClientRemote = {
    if (_instance == null) {
      _instance = new LoginClientRemote(secManagerHost)
      _instance
    }
    else if (secManagerHost == _instance.secManagerHost)
      _instance
    else
      throw new Exception("LoginClientRemote is already initialized with different parameters")
  }

  def instance():LoginClientRemote = {
    if(_instance != null)
      _instance
    else
      throw new Exception("LoginClientRemote not initialized")
  }


}*/


