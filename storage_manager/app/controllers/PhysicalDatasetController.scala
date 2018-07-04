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

import cats.syntax.show.toShowOps
import com.typesafe.config.Config
import daf.dataset.{ DatasetOperations, DatasetParams, FileDatasetParams, KuduDatasetParams }
import daf.filesystem.fileFormatShow
import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkConf
import org.slf4j.{ Logger, LoggerFactory }

class PhysicalDatasetController(sparkSession: SparkSession,
                                kuduMaster: String,
                                override val defaultLimit: Int = 1000,
                                defaultChunkSize: Int = 0) extends DatasetOperations {

  lazy val kuduController = new KuduController(sparkSession, kuduMaster)
  lazy val hdfsController = new HDFSController(sparkSession)

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def kudu(params: KuduDatasetParams, limit: Int = defaultLimit) = {
    logger.debug { s"Reading data from kudu table [${params.table}]" }
    kuduController.readData(params.table).map { _ limit math.min(limit, defaultLimit) }
  }

  def hdfs(params: FileDatasetParams, limit: Int = defaultLimit) = {
    logger.debug { s"Reading data from hdfs at path [${params.path}]" }
    hdfsController.readData(params.path, params.format.show, params.param("separator")).map { _ limit math.min(limit, defaultLimit) }
  }

  def get(params: DatasetParams, limit: Int = defaultLimit) = params match {
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

    val defaultLimit = configuration.getInt("daf.limit_row")

    //private val defaultChunkSize = configuration.getInt("chunk_size").getOrElse(throw new Exception("it shouldn't happen"))

    val sparkConfig = new SparkConf()
    sparkConfig.set("spark.driver.memory", configuration.getString("spark_driver_memory"))

    val sparkSession = SparkSession.builder().master("local").config(sparkConfig).getOrCreate()

    val kuduMaster = configuration.getString("kudu.master")

    System.setProperty("sun.security.krb5.debug", "true")

    new PhysicalDatasetController(sparkSession, kuduMaster)
  }
}