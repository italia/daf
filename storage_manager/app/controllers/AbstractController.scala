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

package controllers

import it.gov.daf.common.authentication.Authentication
import org.apache.hadoop.security.UserGroupInformation
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc._

/**
  * This class authenticates users through LDAP and provides the Hadoop impersonation `proxyUser`
  */
abstract class AbstractController(protected val configuration: Configuration, val playSessionStore: PlaySessionStore) extends Controller {

  UserGroupInformation.loginUserFromSubject(null)
  Authentication(configuration, playSessionStore)
  System.setProperty("javax.security.auth.useSubjectCredsOnly", "false")

  protected implicit val proxyUser = UserGroupInformation.getCurrentUser

}