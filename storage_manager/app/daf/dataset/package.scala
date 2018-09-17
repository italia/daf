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

import akka.stream.scaladsl.{ Framing, Source }
import akka.util.ByteString
import daf.filesystem.{ FileDataFormat, JsonFileFormat }

package object dataset {

  type ExtraParams = Map[String, String]

  private def quote(s: String) = s""""$s""""

  val defaultSeparator = System.lineSeparator
  val jsonSeparator    = s",$defaultSeparator"

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
    */
  def asStringSource(source: Source[ByteString, _]): Source[String, _] = source.via {
    Framing.delimiter(ByteString(defaultSeparator), Int.MaxValue, true)
  }.map { _.utf8String }.filter { _.nonEmpty }

  /**
    * Wraps a JSON string source of which each line is a valid JSON object, into a JSON array.
    * @param source the raw JSON string source
    * @return an array of JSON objects
    */
  def wrapJson(source: Source[String, _]): Source[String, _] = wrapSource(source).sliding(2, 2).map {
    case Seq("<start>", "<end>") => "[]"
    case Seq("<start>", row)     => s"[$defaultSeparator  $row"
    case Seq(row, "<end>")       => s"$jsonSeparator  $row$defaultSeparator]"
    case Seq("<end>")            => s"$defaultSeparator]"
    case Seq(row1)               => s"$jsonSeparator  $row1"
    case Seq(row1, row2)         => s"$jsonSeparator  $row1$jsonSeparator  $row2"
    case rows                    => rows mkString jsonSeparator
  }

  /**
    * Wraps a string source, adding the default separator to each line.
    * @param source the raw string source
    * @return the same data as was in the original source, with a default separator added to each line
    */
  def wrapDefault(source: Source[String, _]): Source[String, _] = source.map { _ + defaultSeparator }

  /**
    * Converts and formats a byte source into a string.
    * @note Each line is separated by the `System.lineSeparator()` by default.
    * @param source the `ByteString` source to be converted and formatted
    * @param targetFormat the target file format
    */
  def formatExport(source: Source[ByteString, _], targetFormat: FileDataFormat) = targetFormat match {
    case JsonFileFormat => wrapJson    { asStringSource(source) }
    case _              => wrapDefault { asStringSource(source) }
  }

}
