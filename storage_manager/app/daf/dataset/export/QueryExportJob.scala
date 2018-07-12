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

package daf.dataset.export

import daf.dataset.ExtraParams
import daf.filesystem._
import org.apache.livy.{ Job, JobContext }
import org.apache.spark.sql._

import scala.util.{ Failure, Success, Try }

/**
  * Livy `Job` for querying tables using Spark-SQL functionality in a hive context.
  * @param query the HiveQL query to execute in Spark
  * @param to the target output format of the query result
  * @param extraParams a map of additional parameters that can be passed to this job, such as a `separator` in cases
  *                    where `to` is [[daf.filesystem.CsvFileFormat]]
  */
class QueryExportJob(val query: String, val to: FileExportInfo, val extraParams: Map[String, String]) extends Job[String] {

  private val csvDelimiter     = extraParams.getOrElse("separator", ",")
  private val csvIncludeHeader = true

  // Export

  private def prepareCsvWriter(writer: DataFrameWriter[Row]) = writer
    .option("header",    csvIncludeHeader)
    .option("delimiter", csvDelimiter)

  private def write(data: DataFrame) = to match {
    case FileExportInfo(path, CsvFileFormat)  => prepareCsvWriter(data.write).csv(path)
    case FileExportInfo(path, JsonFileFormat) => data.write.json(path)
    case FileExportInfo(_, unsupported)       => throw new IllegalArgumentException(s"Output file format [$unsupported] is invalid")
  }

  private def doExport(session: SparkSession) = for {
    data <- Try { session.sql(query) }
    _    <- Try { write(data) }
  } yield ()

  override def call(jobContext: JobContext) = doExport { jobContext.sqlctx().sparkSession } match {
    case Success(_)     => to.path
    case Failure(error) => throw new RuntimeException("Export Job execution failed", error)
  }

}

object QueryExportJob {

  def create(query: String, outputPath: String, to: FileDataFormat, extraParams: ExtraParams = Map.empty[String, String]) = new QueryExportJob(
    query,
    FileExportInfo(outputPath, to),
    extraParams
  )

}