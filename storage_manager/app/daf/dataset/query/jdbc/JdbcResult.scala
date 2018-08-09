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

package daf.dataset.query.jdbc

import java.lang.{ Boolean => JavaBoolean, Double => JavaDouble, Float => JavaFloat, Integer => JavaInteger, Long => JavaLong }
import java.sql.{ Timestamp, Array => JdbcArray, Struct => JdbcStruct }
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import cats.free.Free
import cats.instances.list.catsStdInstancesForList
import cats.instances.try_.catsStdInstancesForTry
import cats.syntax.traverse.toTraverseOps
import daf.dataset.cleanCsv
import play.api.libs.json._

import scala.util.Try

/**
  * Container and bridge class for JDBC results, containing the header information as well as a `Vector` of rows.
  *
  * @note The `rows` have to be eager because the stream implementation causes `StackOverflowError` due to the
  *       stack-based recursion used in the `Stream`'s `append` method. Vector is also faster in appending than other
  *       structures.
  */
case class JdbcResult(header: Header, rows: Vector[Row]) {

  private val index = header.zipWithIndex.map { case (col, i) => i -> col}.toMap[Int, String]

  private def anyJson(value: Any): Trampoline[JsValue] = value match {
    case number: JavaInteger       => Free.pure[Try, JsValue]  { JsNumber(BigDecimal(number)) }
    case number: JavaLong          => Free.pure[Try, JsValue]  { JsNumber(BigDecimal(number)) }
    case number: JavaDouble        => Free.pure[Try, JsValue]  { JsNumber(BigDecimal(number)) }
    case number: JavaFloat         => Free.pure[Try, JsValue]  { JsNumber(BigDecimal.decimal(number)) }
    case boolean: JavaBoolean      => Free.pure[Try, JsValue]  { JsBoolean(Boolean.unbox(boolean)) }
    case s: String                 => Free.pure[Try, JsValue]  { JsString(s) }
    case _: JdbcArray | _: JdbcStruct => Free.defer[Try, JsValue] { complexJson(value) }
    case seq: Seq[_]               => Free.defer[Try, JsValue] { complexJson(seq) }
    case timestamp: Timestamp      => Free.pure[Try, JsValue]  {
      JsString { timestamp.toLocalDateTime.atOffset(ZoneOffset.UTC).format { DateTimeFormatter.ISO_OFFSET_DATE_TIME } }
    }
    case _                         => recursionError[JsValue] { new RuntimeException(s"Unable to convert jdbc value of type [${value.getClass.getName}] to JSON") }
  }

  private def complexJson(value: Any): Trampoline[JsValue] = value match {
    case seq: Seq[_]          => seq.toList.traverse[Trampoline, JsValue] { anyJson }.map { JsArray }
    case array: JdbcArray     => Free.pure[Try, Row] { array.getArray.asInstanceOf[Array[AnyRef]].toList }.flatMap { anyJson }
    case struct: JdbcStruct   => Free.defer[Try, JsValue] { anyJson(struct.getAttributes.toList) }
    case _                    => recursionError[JsValue] { new RuntimeException(s"Unable to convert jdbc value of type [${value.getClass.getName}] to JSON") }
  }

  private def json(row: Row) = row.traverse[Trampoline, JsValue] { anyJson }.map { values =>
    JsObject {
      values.zipWithIndex.map { case (v, i) => index(i) -> v }
    }
  }

  /**
    * Converts the data contained in this instance into a `Stream` of CSV data.
    */
  def toCsv: Vector[String] =
    header.map { h => s""""$h"""" }.mkString(", ") +:
    rows.map { _.map { cleanCsv }.mkString(", ") }

  /**
    * Converts the data contained in this instance into a `Stream` of JSON data.
    */
  def toJson: Vector[JsObject] = rows.map { json(_).runTailRec.get } // let throw in case of error
}

