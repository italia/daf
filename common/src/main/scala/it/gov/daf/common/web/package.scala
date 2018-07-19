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

package it.gov.daf.common

import cats.{ Applicative, ApplicativeError, Id, ~> }
import org.slf4j.Logger
import play.api.mvc.{ Request, Result }

import scala.concurrent.Future
import scala.language.higherKinds
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

package object web {

  type Authorization = String
  type UserId        = String

  type WebAction[F[_], A] = Request[A] => F[Result]
  type SyncWebAction[A]   = WebAction[Id, A]
  type AsyncWebAction[A]  = WebAction[Future, A]
  type SafeWebAction[A]   = WebAction[Try, A]

  type SecureWebAction[F[_], A] = (Request[A], Authorization, UserId) => F[Result]
  type SyncSecureWebAction[A]   = SecureWebAction[Id, A]
  type AsyncSecureWebAction[A]  = SecureWebAction[Future, A]
  type SafeSecureWebAction[A]   = SecureWebAction[Try, A]

  type ErrorHandler = PartialFunction[Throwable, Result]
  type DefiniteErrorHandler = Throwable => Result

  implicit class SyncWebActionSyntax[A](action: SyncWebAction[A]) {

    /**
      * Points this [[SyncWebAction]] into another that accepts error handling.
      * @note When `F` is `Future`, an implicit execution context is required
      * @tparam F the type to point this `Id` to
      * @return a new `WebAction` that wraps its result into `F`
      */
    def attempt[F[_]](implicit Ap: ApplicativeError[F, Throwable]): WebAction[F, A] = { request =>
      Ap.fromTry {
        Try { action(request) }
      }
    }
  }

  implicit class SyncSecureWebActionSyntax[A](action: SyncSecureWebAction[A]) {

    /**
      * Points this [[SyncSecureWebAction]] into another that accepts error handling.
      * @note when `F` is `Future`, an implicit execution context is required
      * @tparam F the type to point this `Id` to
      * @return a new `WebAction` that wraps its result into `F`
      */
    def attempt[F[_]](implicit Ap: ApplicativeError[F, Throwable]): SecureWebAction[F, A] = { (request, auth, userId) =>
      Ap.fromTry {
        Try { action(request, auth, userId) }
      }
    }
  }

  implicit class WebActionSyntax[F[_], A](action: WebAction[F, A]) {

    /**
      * Applies a natural transformation from `F` to `G`.
      * @return a [[WebAction]] whose result is wrapped in `G`
      */
    def transform[G[_]](implicit nat: F ~> G): WebAction[G, A] = { request => nat(action(request)) }

    /**
      * Runs this action, catching any exception thrown by the application of the action itself.
      * @note if `F` is a structure capable of error handling, then the errors internal to `F` are '''not''' handled by
      *       this function
      * @see [[recover]]
      * @param onError the [[ErrorHandler]] to apply in case of error
      * @return a [[WebAction]] of the same type as the input, whose result is that of applying `onError` in case of
      *         error
      */
    def attempt(onError: ErrorHandler)(implicit Ap: Applicative[F]): WebAction[F, A] = { request =>
      Try { action(request) } match {
        case Success(result) => result
        case Failure(error)  => Ap.pure { onError.applyOrElse(error, ErrorHandlers.fallback) }
      }
    }

    /**
      * Runs this action, handling errors '''internal''' to `F`.
      * @param onError the [[ErrorHandler]] to apply in case of error
      * @return a [[WebAction]] of the same type as the input, whose result is that of applying `onError` in case of
      *         error
      */
    def recover(onError: ErrorHandler)(implicit Ap: ApplicativeError[F, Throwable]): WebAction[F, A] = action.andThen {
      Ap.handleError(_) { onError.applyOrElse(_, ErrorHandlers.fallback) }
    }

  }

  implicit class SecureWebActionSyntax[F[_], A](action: SecureWebAction[F, A]) {

    /**
      * Applies a natural transformation from `F` to `G`.
      * @return a [[SecureWebAction]] whose result is wrapped in `G`
      */
    def transform[G[_]](implicit nat: F ~> G): SecureWebAction[G, A] = { (request, auth, userId) => nat(action(request, auth, userId)) }

    /**
      * Runs this action, catching any exception thrown by the application of the action itself.
      * @note if `F` is a structure capable of error handling, then the errors internal to `F` are '''not''' handled by
      *       this function
      * @see [[recover]]
      * @param onError the [[ErrorHandler]] to apply in case of error
      * @return a [[WebAction]] of the same type as the input, whose result is that of apply `onError` in case of error
      */
    def attempt(onError: ErrorHandler)(implicit Ap: Applicative[F]): SecureWebAction[F, A] = { (request, auth, userId) =>
      Try { action(request, auth, userId) } match {
        case Success(result) => result
        case Failure(error)  => Ap.pure { onError(error) }
      }
    }

    /**
      * Runs this action, handling errors '''internal''' to `F`.
      * @param onError the [[ErrorHandler]] to apply in case of error
      * @return a [[WebAction]] of the same type as the input, whose result is that of applying `onError` in case of
      *         error
      */
    def recover(onError: ErrorHandler)(implicit Ap: ApplicativeError[F, Throwable]): SecureWebAction[F, A] = { (request, auth, userId) =>
      Ap.handleError(action(request, auth, userId)) { onError.applyOrElse(_, ErrorHandlers.fallback) }
    }

  }

  implicit class ErrorHandlerSyntax(handler: ErrorHandler) {

    /**
      * Applies logging to a [[DefiniteErrorHandler]] that doesn't already do so.
      */
    def logged(implicit logger: Logger): ErrorHandler = {
      case error if handler isDefinedAt error => logger.debug("A caught exception was encountered", error); handler(error)
    }

  }

  implicit class DefiniteErrorHandlerSyntax(handler: DefiniteErrorHandler) {

    private def defaultLogger(logger: Logger): Throwable => Unit = {
      case NonFatal(error) => logger.error("A non fatal exception occurred", error)
      case error           => logger.error("A fatal exception has occurred!", error)
    }

    /**
      * Applies logging to a [[DefiniteErrorHandler]] that doesn't already do so. The logging applied with log messages
      * separately for non-fatal and fatal exceptions.
      */
    def logged(implicit logger: Logger): DefiniteErrorHandler = { error =>
      defaultLogger(logger)(error)
      handler(error)
    }

  }

}
