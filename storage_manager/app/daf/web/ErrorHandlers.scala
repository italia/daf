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

package daf.web

import java.io.FileNotFoundException
import java.lang.reflect.UndeclaredThrowableException

import it.gov.daf.common.web.ErrorHandler
import org.apache.spark.sql.AnalysisException
import org.ietf.jgss.GSSException
import play.api.mvc.Results

object ErrorHandlers {

  val security: ErrorHandler = {
    case _: GSSException => Results.Unauthorized
  }

  val spark: ErrorHandler = {
    case _: FileNotFoundException => Results.NotFound
    case _: AnalysisException     => Results.NotFound
    case error: UndeclaredThrowableException if error.getUndeclaredThrowable.isInstanceOf[AnalysisException] => Results.NotFound
  }

}
