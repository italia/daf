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

import daf.filesystem.{ CsvFileFormat, FileDataFormat, JsonFileFormat }
import org.apache.livy.{ Job, JobContext }
import org.apache.kudu.spark.kudu._
import org.apache.spark.sql._

import scala.util.{ Failure, Success, Try }

/**
  * Livy `Job` for converting a Kudu table to CSV or JSON.
  *
  * @param table the name of the Kudu table to export
  * @param to details representing the output data
  * @param master the location of the Kudu master
  */
class KuduExportJob(val table: String, val master: String, val to: FileExportInfo) extends Job[String] {

  private val csvDelimiter     = ","
  private val csvIncludeHeader = true
  private val csvInferSchema   = true

  private def prepareCsvWriter(writer: DataFrameWriter[Row]) = writer
    .option("header",    csvIncludeHeader)
    .option("delimiter", csvDelimiter)

  private def prepareReader(reader: DataFrameReader) = reader
    .option("kudu.master", master)
    .option("kudu.table", table)

  private def read(session: SparkSession) = prepareReader { session.read }.kudu

  private def write(data: DataFrame) = to match {
    case FileExportInfo(path, CsvFileFormat)  => prepareCsvWriter(data.write).csv(path)
    case FileExportInfo(path, JsonFileFormat) => data.write.json(path)
    case FileExportInfo(_, unsupported)       => throw new IllegalArgumentException(s"Output file format [$unsupported] is invalid")
  }

  private def doExport(session: SparkSession) = for {
    data <- Try { read(session) }
    _    <- Try { write(data) }
  } yield ()

  def call(jobContext: JobContext) = doExport { jobContext.sqlctx().sparkSession } match {
    case Success(_)     => to.path
    case Failure(error) => throw new RuntimeException("Export Job execution failed", error)
  }

}

object KuduExportJob {

  def create(table: String, master: String, outputPath: String, outputFormat: FileDataFormat) = new KuduExportJob(
    table,
    master,
    FileExportInfo(outputPath, outputFormat)
  )

}