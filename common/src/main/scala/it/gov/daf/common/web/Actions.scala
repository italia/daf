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

import cats.{ Applicative, Id }
import cats.instances.future.catsStdInstancesForFuture
import cats.instances.try_.catsStdInstancesForTry
import it.gov.daf.common.authentication.Authentication
import it.gov.daf.common.utils.tryFutureNat
import org.slf4j.{ Logger, LoggerFactory }
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds
import scala.util.Try

/**
  * Utility object to help with the creation of actions that perform common tasks. Intended to help alleviate the
  * requirement on the use of closures to wrap actions for error checking, user impersonation and so on.
  *
  * @note The returned [[ActionBuilder]] instance is applicable as a function that can be used to build a Play
  *       synchronous `Action`.
  */
object Actions {

  /**
    * Returns a builder that handles basic errors and applies no impersonation
    * @see [[ErrorHandlers.basic]], [[ErrorHandlers.security]] and [[Impersonations.none]]
    * @return a builder that handles basic errors and applies no impersonation
    */
  def basic: ActionBuilder = ActionBuilder.default.checkedWith { ErrorHandlers.security }

  /**
    * Creates a builder that handles basic errors but applies the given impersonation strategy
    * @param impersonation the impersonation strategy to apply on the wrapped action
    * @return a builder that handles basic errors but applies the given impersonation strategy
    */
  def impersonated(impersonation: Impersonation): ActionBuilder = basic.impersonatedWith(impersonation)

}

sealed case class ActionBuilder(protected val errorHandler: ErrorHandler,
                                protected val impersonation: Impersonation,
                                protected implicit val logger: Logger) {

  private val loggedErrorHandler = errorHandler.logged

  private def withUserData[F[_], A](action: String => WebAction[F, A]): WebAction[F, A] = { request =>
    action { Authentication.getProfiles(request).headOption.map { _.getId } getOrElse "anonymous" } apply request
  }

  private def impersonate[F[_], A](action: WebAction[F, A]): WebAction[F, A] = withUserData { impersonation.wrap(action) }

  private def wrap[F[_], A](action: WebAction[F, A])(implicit Ap: Applicative[F]): WebAction[F, A] = impersonate(action).attempt { loggedErrorHandler }

  private def wrap[F[_]](result: => F[Result])(implicit Ap: Applicative[F]): WebAction[F, AnyContent] = wrap[F, AnyContent] { _ => result }

  private def secure[F[_], A](action: SecureWebAction[F, A])(implicit Ap: Applicative[F]): WebAction[F, A] = { request =>
    request.headers.get("Authorization") match {
      case None       => Ap.pure { Results.Unauthorized }
      case Some(auth) => withUserData { userId =>
        wrap[F, A] { action(_, auth, userId) }
      } apply request
    }
  }

  private def id[F[_], A](action: WebAction[F, A]): WebAction[F, A] = action

  /**
    * Adds an error handler at the end of the current list of handlers.
    * @note The fallback handler is applied automatically
    * @param handler the [[ErrorHandler]] to add
    * @return a copy of this builder, including the new handler
    */
  def checkedWith(handler: ErrorHandler): ActionBuilder = this.copy(
    errorHandler = this.errorHandler orElse handler
  )

  /**
    * Sets the given impersonator for this builder.
    * @param newImpersonation the new impersonation strategy
    * @return a copy of this builder, with the new impersonation strategy
    */
  def impersonatedWith(newImpersonation: Impersonation): ActionBuilder = this.copy(
    impersonation = newImpersonation
  )

  // Un-Secured calls

  def apply[A](bodyParser: BodyParser[A])(action: SyncWebAction[A]): Action[A] = Action(bodyParser) { wrap[Id, A](action) }

  def async[A](bodyParser: BodyParser[A])(action: AsyncWebAction[A])(implicit executionContext: ExecutionContext): Action[A] = Action.async(bodyParser) {
    wrap[Future, A](action).recover { loggedErrorHandler }
  }

  def attempt[A](bodyParser: BodyParser[A])(action: SafeWebAction[A]): Action[A] = Action.async(bodyParser) {
    wrap[Try, A](action).recover { loggedErrorHandler }.transform[Future]
  }

  def apply(action: SyncWebAction[AnyContent]): Action[AnyContent] = Action { wrap[Id, AnyContent](action) }


  def async(action: AsyncWebAction[AnyContent])(implicit executionContext: ExecutionContext): Action[AnyContent] = Action.async {
    wrap[Future, AnyContent](action).recover { loggedErrorHandler }
  }

  def attempt(action: WebAction[Try, AnyContent]): Action[AnyContent] = Action.async {
    wrap[Try, AnyContent](action).recover { loggedErrorHandler }.transform[Future]
  }

  def async(action: => Future[Result])(implicit executionContext: ExecutionContext): Action[AnyContent] = Action.async {
    wrap[Future](action).recover { loggedErrorHandler }
  }

  // Secured calls

  def secured(action: SyncSecureWebAction[AnyContent]): Action[AnyContent] = Action { secure[Id, AnyContent](action) }

  def securedAttempt(action: SecureWebAction[Try, AnyContent]): Action[AnyContent] = Action.async {
    secure[Try, AnyContent](action).recover { loggedErrorHandler }.transform[Future]
  }

  def securedAsync(action: AsyncSecureWebAction[AnyContent])(implicit executionContext: ExecutionContext): Action[AnyContent] = Action.async {
    secure[Future, AnyContent](action).recover { loggedErrorHandler }
  }

  def secured[A](bodyParser: BodyParser[A])(action: SyncSecureWebAction[A]): Action[A] = Action(bodyParser) { secure[Id, A](action) }

  def securedAttempt[A](bodyParser: BodyParser[A])(action: SafeSecureWebAction[A]): Action[A] = Action.async(bodyParser) {
    secure[Try, A](action).recover { loggedErrorHandler }.transform[Future]
  }

  def securedAsync[A](bodyParser: BodyParser[A])(action: AsyncSecureWebAction[A])(implicit executionContext: ExecutionContext): Action[A] = Action.async(bodyParser) {
    secure[Future, A](action).recover { loggedErrorHandler }
  }

}

private object ActionBuilder {

  val logger = LoggerFactory.getLogger("it.gov.daf.Action")

  def default = apply(ErrorHandlers.basic, Impersonations.none, logger)

}
