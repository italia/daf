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

import it.gov.daf.common.sso.UserGroupInformationSyntax
import org.apache.hadoop.security.UserGroupInformation

import scala.language.higherKinds

trait Impersonation {

  def wrap[F[_], A](action: WebAction[F, A]): String => WebAction[F, A]

}

sealed class HadoopImpersonation(proxyUser: UserGroupInformation) extends Impersonation {

  def wrap[F[_], A](action: WebAction[F, A]) = { userId => request =>
    (proxyUser as userId) { action(request) }
  }

}

object NoImpersonation extends Impersonation {

  def wrap[F[_], A](action: WebAction[F, A]) = { _ => action }
}

object Impersonations {

  def none: Impersonation = NoImpersonation

  /**
    * An `Action` will be performed by the `proxyUser` on behalf of the supplied user id. The `proxyUser` can be any
    * instance of `UserGroupInformation`, such as the current user, the logged in user, a user logged in from a Kerberos
    * keytab, a subject and so on.
    * @param proxyUser the user who will do the impersonation
    */
  def hadoop(implicit proxyUser: UserGroupInformation): Impersonation = new HadoopImpersonation(proxyUser)

}