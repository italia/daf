package it.teamdigitale.filesystem.export

import it.teamdigitale.filesystem._
import org.apache.hadoop.fs.Path
import org.apache.livy.{ Job, JobContext }
import org.apache.spark.sql._

import scala.util.{ Failure, Success, Try }

/**
  * Livy [[Job]] for converting a file from CSV, Parquet or JSON to CSV or JSON. Note that if the source and destination
  * formats are identical, the Spark Job is '''still''' triggered.
  * @param from details representing the input data
  * @param to details representing the output data
  */
class FileExportJob(val from: FileExportInfo, val to: FileExportInfo) extends Job[String] {

  private val csvDelimiter     = ","
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
    case FileExportInfo(path, RawFileFormat)     => prepareCsvReader(session.read).csv(path)
    case FileExportInfo(path, ParquetFileFormat) => session.read.parquet(path)
    case FileExportInfo(path, JsonFileFormat)    => session.read.json(path)
    case FileExportInfo(_, unsupported)          => throw new IllegalArgumentException(s"Input file format [$unsupported] is invalid")
  }

  private def write(data: DataFrame) = to match {
    case FileExportInfo(path, RawFileFormat)  => prepareCsvWriter(data.write).csv(path)
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

  def create(inputPath: String, outputPath: String, from: FileDataFormat, to: FileDataFormat) = new FileExportJob(
    FileExportInfo(inputPath, from),
    FileExportInfo(outputPath, to)
  )

}

case class FileExportInfo(path: String, format: FileDataFormat)

object FileExportInfo {

  def apply(path: Path, format: FileDataFormat): FileExportInfo = apply(path.toUri.getPath, format)

}
