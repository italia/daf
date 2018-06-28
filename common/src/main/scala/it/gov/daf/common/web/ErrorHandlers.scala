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

import java.nio.file.InvalidPathException
import java.security.AccessControlException

import org.slf4j.{ Logger, LoggerFactory }
import play.api.mvc.Results

object ErrorHandlers {

  val logger: Logger = LoggerFactory.getLogger("it.gov.daf.ErrorHandler")

  /**
    * A basic error handler that captures only a few messages such as `NotImplementedError`.
    */
  val basic: ErrorHandler = {
    case _: NotImplementedError          => Results.NotImplemented
    case error: InvalidPathException     => Results.BadRequest { error.getMessage }
  }

  /**
    * A default fallback that simply returns `InternalServerError` for any exception encountered.
    * @note this handler does not log exceptions.
    */
  val defaultFallback: DefiniteErrorHandler = { _ =>
    Results.InternalServerError { "An unexpected error has occurred" }
  }

  /**
    * A default fallback that acts inherits from [[defaultFallback]] but adds logging.
    */
  val fallback: DefiniteErrorHandler = defaultFallback.logged(logger)

  /**
    * A handler for basic security exceptions.
    */
  val security: ErrorHandler = {
    case _: AccessControlException => Results.Unauthorized
  }

}
