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

import java.lang.reflect.UndeclaredThrowableException
import java.nio.file.InvalidPathException
import java.security.PrivilegedExceptionAction

import com.google.inject.Inject
import it.gov.daf.common.authentication.Authentication
import it.teamdigitale.PhysicalDatasetController
import org.apache.hadoop.security.{AccessControlException, UserGroupInformation}
import org.apache.spark.sql.AnalysisException
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import play.mvc.Http

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * This class authenticates users through LDAP and provides Hadoop impersonation
  * @param configuration
  * @param playSessionStore
  */
abstract class AbstractController @Inject()(configuration: Configuration, val playSessionStore: PlaySessionStore) extends Controller {

  UserGroupInformation.loginUserFromSubject(null)

  private val proxyUser = UserGroupInformation.getCurrentUser

  Authentication(configuration, playSessionStore)

  protected val exceptionManager: PartialFunction[Throwable, Result] = (exception: Throwable) => exception match {
    case ex: AnalysisException =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.NOT_FOUND, Map.empty))
    case ex: NotImplementedError =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.NOT_IMPLEMENTED, Map.empty))
    case ex: UndeclaredThrowableException if ex.getUndeclaredThrowable.isInstanceOf[AnalysisException] =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.NOT_FOUND, Map.empty))
    case ex: InvalidPathException =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.BAD_REQUEST, Map.empty))
    case ex: Throwable =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.INTERNAL_SERVER_ERROR, Map.empty))
  }

  protected val hadoopExceptionManager: PartialFunction[Throwable, Result] = (exception: Throwable) => exception match {
    case ex: AccessControlException =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.UNAUTHORIZED, Map.empty))
  }

  def CheckedAction(exceptionManager: Throwable => Result)(action: Request[AnyContent] => Result) = (request: Request[AnyContent]) => {
    Try(action(request)) match {
      case Success(response) => response
      case Failure(exception) => exceptionManager(exception)
    }
  }


  /**
    * Impersonation (proxy user) as a service
    * @param action
    * @return
    */
  def HadoopDoAsAction(action: Request[AnyContent] => Result) = (request: Request[AnyContent]) => {
    val profiles = Authentication.getProfiles(request)
    val user = profiles.headOption.map(_.getId).getOrElse("anonymous")
    val ugi = UserGroupInformation.createProxyUser(user, proxyUser)
    ugi.doAs(new PrivilegedExceptionAction[Result]() {
      override def run: Result = action(request)
    })
  }


}
