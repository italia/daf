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

import cats.syntax.show.toShow
import com.typesafe.config.Config
import daf.dataset.{ DatasetParams, FileDatasetParams, KuduDatasetParams }
import daf.filesystem.fileFormatShow
import org.apache.spark.sql.{ DataFrame, SparkSession }
import org.apache.spark.SparkConf
import org.slf4j.{ Logger, LoggerFactory }

class PhysicalDatasetController(sparkSession: SparkSession,
                                kuduMaster: String,
                                defaultLimit: Option[Int] = None,
                                defaultChunkSize: Int = 0) {

  lazy val kuduController = new KuduController(sparkSession, kuduMaster)
  lazy val hdfsController = new HDFSController(sparkSession)

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def addLimit(dataframe: DataFrame, limit: Option[Int]) = (limit, defaultLimit) match {
    case (None, None)                 => dataframe
    case (None, Some(value))          => dataframe.limit { value }
    case (Some(value), None)          => dataframe.limit { value }
    case (Some(value), Some(default)) => dataframe.limit { math.min(value, default) }
  }

  def kudu(params: KuduDatasetParams, limit: Option[Int] = None) = {
    logger.debug { s"Reading data from kudu table [${params.table}]" }
    kuduController.readData(params.table).map { addLimit(_, limit) }
  }

  def hdfs(params: FileDatasetParams, limit: Option[Int] = None) = {
    logger.debug { s"Reading data from hdfs at path [${params.path}]" }
    hdfsController.readData(params.path, params.format.show, params.param("separator")).map { addLimit(_, limit) }
  }

  def get(params: DatasetParams, limit: Option[Int]= None) = params match {
    case kuduParams: KuduDatasetParams => kudu(kuduParams, limit)
    case hdfsParams: FileDatasetParams => hdfs(hdfsParams, limit)
  }

}

object PhysicalDatasetController {

  private def getOptionalString(path: String, underlying: Config) = {
    if (underlying.hasPath(path)) {
      Some(underlying.getString(path))
    } else {
      None
    }
  }

  private def getOptionalInt(path: String, underlying: Config) = {
    if (underlying.hasPath(path)) {
      Some(underlying.getInt(path))
    } else {
      None
    }
  }

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def apply(configuration: Config): PhysicalDatasetController = {

    val sparkConfig = new SparkConf()
    sparkConfig.set("spark.driver.memory", configuration.getString("spark.driver.memory"))

    val sparkSession = SparkSession.builder().master("local").config(sparkConfig).getOrCreate()

    val kuduMaster = configuration.getString("kudu.master")

    val defaultLimit = if (configuration hasPath "daf.row_limit") Some {
      configuration.getInt("daf.row_limit")
    } else None

    System.setProperty("sun.security.krb5.debug", "true")

    new PhysicalDatasetController(sparkSession, kuduMaster, defaultLimit)
  }
}