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

package it.gov.daf.common.web

import it.gov.daf.common.authentication.Authentication
import it.gov.daf.common.sso.config.KerberosConfig
import org.apache.hadoop.security.UserGroupInformation
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc._

import scala.util.{ Failure, Success, Try }

/**
  * This class authenticates users through a keytab and provides the Hadoop impersonation `proxyUser`.
  */
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Throw"
  )
)
abstract class SecureController(protected val configuration: Configuration, val playSessionStore: PlaySessionStore) extends Controller {

  private def prepareEnv() = Try { System.setProperty("javax.security.auth.useSubjectCredsOnly", "false") }

  private def loginUserFromConf = KerberosConfig.reader.map { config =>
    UserGroupInformation.loginUserFromKeytab(config.principal, config.keytab)
  }

  private def prepareAuth() = Try { Authentication(configuration, playSessionStore) }

  private def initUser() = for {
    _ <- prepareEnv()
    _ <- loginUserFromConf.read { configuration }
    _ <- prepareAuth()
  } yield UserGroupInformation.getLoginUser

  protected implicit val proxyUser: UserGroupInformation = initUser() match {
    case Success(null)  => throw new RuntimeException("Unable to initialize user for application")
    case Failure(error) => throw new RuntimeException("Unable to initialize user for application", error)
    case Success(user)  => user
  }

}

