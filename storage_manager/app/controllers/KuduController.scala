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

import org.apache.kudu.spark.kudu._
import org.apache.spark.sql.{ DataFrame, SparkSession }
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Try }

class KuduController(sparkSession: SparkSession, master: String) {

  val alogger: Logger = LoggerFactory.getLogger(this.getClass)

  def readData(table: String): Try[DataFrame] =  {
    Try{sparkSession
      .sqlContext
      .read
      .options(Map("kudu.master" -> master, "kudu.table" -> table)).kudu
    } recoverWith {
      case ex =>
        alogger.error(s"Exception ${ex.getMessage}\n ${ex.getStackTrace.mkString("\n")} ")
        Failure(ex)
    }
  }
}
