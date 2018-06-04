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
import org.apache.hadoop.fs.Path
import org.apache.livy.{ Job, JobContext }
import org.apache.spark.sql._

import scala.util.{ Failure, Success, Try }

/**
  * Livy `Job` for converting a file from CSV, Parquet or JSON to CSV or JSON. Note that if the source and destination
  * formats are identical, the Spark Job is '''still''' triggered.
  * @param from details representing the input data
  * @param to details representing the output data
  * @param extraParams a map of additional parameters that can be passed to this job, such as a `separator` in cases
  *                    where `to` is [[daf.filesystem.CsvFileFormat]]
  */
class FileExportJob(val from: FileExportInfo, val to: FileExportInfo, val extraParams: Map[String, String]) extends Job[String] {

  private val csvDelimiter     = extraParams.getOrElse("separator", ",")
  private val csvIncludeHeader = true
  private val csvInferSchema   = true

  // Export

  private def prepareCsvReader(reader: DataFrameReader) = reader
    .option("inferSchema", csvInferSchema)
    .option("header",      csvIncludeHeader)
    .option("delimiter",   csvDelimiter)

  private def prepareCsvWriter(writer: DataFrameWriter[Row]) = writer
    .option("header",    csvIncludeHeader)
    .option("delimiter", csvDelimiter)

  private def read(session: SparkSession) = from match {
    case FileExportInfo(path, RawFileFormat | CsvFileFormat) => prepareCsvReader(session.read).csv(path)
    case FileExportInfo(path, ParquetFileFormat)             => session.read.parquet(path)
    case FileExportInfo(path, JsonFileFormat)                => session.read.json(path)
    case FileExportInfo(_, unsupported)                      => throw new IllegalArgumentException(s"Input file format [$unsupported] is invalid")
  }

  private def write(data: DataFrame) = to match {
    case FileExportInfo(path, CsvFileFormat)  => prepareCsvWriter(data.write).csv(path)
    case FileExportInfo(path, JsonFileFormat) => data.write.json(path)
    case FileExportInfo(_, unsupported)       => throw new IllegalArgumentException(s"Output file format [$unsupported] is invalid")
  }

  private def doExport(session: SparkSession) = for {
    data <- Try { read(session) }
    _    <- Try { write(data) }
  } yield ()

  override def call(jobContext: JobContext) = doExport { jobContext.sqlctx().sparkSession } match {
    case Success(_)     => to.path
    case Failure(error) => throw new RuntimeException("Export Job execution failed", error)
  }

}

object FileExportJob {

  def create(inputPath: String, outputPath: String, from: FileDataFormat, to: FileDataFormat, extraParams: ExtraParams = Map.empty[String, String]) = new FileExportJob(
    FileExportInfo(inputPath, from),
    FileExportInfo(outputPath, to),
    extraParams
  )

}

case class FileExportInfo(path: String, format: FileDataFormat)

object FileExportInfo {

  def apply(path: Path, format: FileDataFormat): FileExportInfo = apply(path.toUri.getPath, format)

}
