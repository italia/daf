package it.teamdigitale

import com.typesafe.config.Config
import org.apache.spark.sql.{ DataFrame, SparkSession }
import org.apache.spark.SparkConf
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{ Failure, Try }

class PhysicalDatasetController(sparkSession: SparkSession,
                                kuduMaster: String,
                                override val defaultLimit: Int = 1000,
                                defaultChunkSize: Int = 0) extends DatasetOperations {

  lazy val kudu = new KuduController(sparkSession, kuduMaster)
  lazy val hdfs = new HDFSController(sparkSession)
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def toTry[T](option: Option[Try[DataFrame]], errorMessage: String): Try[DataFrame] = option match {
    case Some(d) => d
    case None => Failure(new IllegalArgumentException(errorMessage))
  }

  def get(params: Map[String, String]): Try[DataFrame] = {

    val l: Int = params.get("limit").map(_.toInt).getOrElse(defaultLimit)
    val limit =
      if (l > defaultLimit) defaultLimit
      else l

    params.getOrElse("protocol", "hdfs") match {

      case "kudu" =>

        val tableOp = params.get("table")
        logger.info(s"Reading request for kudu with params: table:$tableOp")

        val res = tableOp.map(kudu.readData(_).map(_.limit(limit)))
        toTry(res, "Table should be defined")

      case "hdfs" =>

        val format = params.getOrElse("format", "parquet")
        val pathOp = params.get("path")
        val separator = params.get("separator")
        logger.info(s"Reading request for hdfs with params: path:$pathOp format: $format")

        val res = pathOp.map(hdfs.readData(_, format, separator).map(_.limit(limit)))
        toTry(res, "Path should be defined")

      case other =>
        logger.info(s"Reading request for $other is still not supported")
        Failure(new IllegalArgumentException(s"$other is still not supported."))

    }

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