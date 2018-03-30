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



  def login(loginInfo:LoginInfo, client: WSClient):Future[Cookie] = {

    require(secManagerHost!=null, "Security Manager host must be provided")

    SsoServiceClient.retriveCookieInternal(secManagerHost,loginInfo.user,loginInfo.appName,client)

  }


}



