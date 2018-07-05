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

package daf.dataset

import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import controllers.PhysicalDatasetController
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

import scala.util.Try

class DatasetService(config: Config) {

  private val storageClient = PhysicalDatasetController(config)

  def schema(params: DatasetParams): Try[StructType] = storageClient.get(params, 1).map { _.schema }

  def data(params: DatasetParams): Try[DataFrame] = storageClient.get(params)

  def jsonData(params: DatasetParams) = data(params).map { json }

  def json(dataFrame: DataFrame) = Source[String] { dataFrame.toJSON.collect().toVector }.map { row => s"$row${System.lineSeparator}" }

//  This code produces valid JSON but is inconsistent with Spark's JSON structure
//  def json(dataFrame: DataFrame) = Source[String] { "<start>" +: dataFrame.toJSON.collect().toVector :+ "<end>"}.sliding(2, 2).map {
//    case Seq("<start>", "<end>") => "[]"
//    case Seq("<start>", row) => s"[${System.lineSeparator()}  $row"
//    case Seq(row, "<end>")   => s",${System.lineSeparator()}  $row${System.lineSeparator()}]"
//    case Seq("<end>")        => s"${System.lineSeparator()}]"
//    case Seq(row1, row2)     => s",${System.lineSeparator()}  $row1,${System.lineSeparator}  $row2"
//    case rows                => rows.map { row => s",${System.lineSeparator()}  $row" }.mkString
//  }

  // TODO: split code without breaking Spark task serialization
  def csvData(params: DatasetParams) = data(params).map { csv }

  def csv(dataFrame: DataFrame) = Source[String] {
    dataFrame.schema.fieldNames.map { h => s""""$h"""" }.mkString(",") +:
    dataFrame.rdd.map { _.toSeq.map { cleanCsv }.mkString(",") }.collect().toVector
  }.map { row => s"$row${System.lineSeparator}" }

}
