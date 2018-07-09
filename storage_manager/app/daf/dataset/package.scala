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

package daf

import java.sql.Timestamp
import java.time.{ LocalDateTime, ZoneOffset }
import java.time.format.DateTimeFormatter
import java.util.Date

package object dataset {

  type ExtraParams = Map[String, String]

  private def quote(s: String) = s""""$s""""

  /**
    * Converts anything into a string representation that may be used in CSV data.
    * @note In case `any` is `null`, the output is a string {{{"<null>"}}}.
    *       Dates are also formatted to ISO standard, with `T` separator, zoned at UTC.
    */
  def cleanCsv(any: Any): String = any match {
    case null         => "<null>"
    case s: String    => quote { s.replaceAll("\"", "\\\"") }
    case t: Timestamp => quote {
      t.toLocalDateTime.atOffset(ZoneOffset.UTC).format { DateTimeFormatter.ISO_OFFSET_DATE_TIME }
    }
    case d: Date      => quote {
      LocalDateTime.from(d.toInstant).atOffset(ZoneOffset.UTC).format { DateTimeFormatter.ISO_OFFSET_DATE }
    }
    case d            => d.toString
  }

}