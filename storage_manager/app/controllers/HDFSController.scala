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

import com.databricks.spark.avro._
import org.apache.spark.sql.{ DataFrame, SparkSession }
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Try}

class HDFSController(sparkSession: SparkSession) {

  val alogger: Logger = LoggerFactory.getLogger(this.getClass)

  def readData(path: String, format: String, separator: Option[String]): Try[DataFrame] =  format match {
    case "csv" => Try {
      val pathFixAle = path + "/" + path.split("/").last + ".csv"
      alogger.debug(s"questo e' il path $pathFixAle")
      separator match {
        case None => sparkSession.read.csv(pathFixAle)
        case Some(sep) => sparkSession.read.format("csv")
          .option("sep", sep)
          .option("inferSchema", "true")
          .option("header", "true")
          .load(pathFixAle)
      }
    }
    case "parquet" => Try { sparkSession.read.parquet(path) }
    case "avro"    => Try { sparkSession.read.avro(path) }
    case unknown   => Failure { new IllegalArgumentException(s"Unsupported format [$unknown]") }
  }
}
