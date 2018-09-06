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

import akka.stream.scaladsl.{ Concat, Source }
import akka.util.ByteString
import daf.filesystem.{ FileDataFormat, JsonFileFormat }

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

  private val streamStart = Source.single { "<start>" }
  private val streamEnd   = Source.single { "<end>"   }

  private def wrapSource(source: Source[String, _]) = streamStart concat source concat streamEnd

  /**
    * Converts a source of bytes into source of strings.
    * @note Each line is separated by the `System.lineSeparator()` by default.
    * @param source the `ByteString` source to be converted
    * @param separator the string used to split each `ByteString`
    */
  def asStringSource(source: Source[ByteString, _], separator: String = System.lineSeparator()): Source[String, _] = source.mapConcat {
    _.utf8String.split { System.lineSeparator() }.toList
  }

  /**
    * Wraps a JSON string source of which each line is a valid JSON object, into a JSON array.
    * @param source the raw JSON string source
    * @return an array of JSON objects
    */
  def wrapJson(source: Source[String, _]): Source[String, _] = wrapSource(source).sliding(2, 2).map {
    case Seq("<start>", "<end>") => "[]"
    case Seq("<start>", row) => s"[${System.lineSeparator()}  $row"
    case Seq(row, "<end>")   => s",${System.lineSeparator()}  $row${System.lineSeparator()}]"
    case Seq("<end>")        => s"${System.lineSeparator()}]"
    case Seq(row1, row2)     => s",${System.lineSeparator()}  $row1,${System.lineSeparator}  $row2"
    case rows                => rows.map { row => s",${System.lineSeparator()}  $row" }.mkString
  }

  /**
    * Converts and formats a byte source into a string.
    * @note Each line is separated by the `System.lineSeparator()` by default.
    * @param source the `ByteString` source to be converted and formatted
    * @param targetFormat the target file format
    * @param separator the string used to split each `ByteString`
    */
  def formatExport(source: Source[ByteString, _], targetFormat: FileDataFormat, separator: String = System.lineSeparator()) = targetFormat match {
    case JsonFileFormat => wrapJson { asStringSource(source) }
    case _              => asStringSource(source).map { s => s"$s${System.lineSeparator()}" }
  }

}
