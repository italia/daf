package it.teamdigitale
import com.typesafe.config.Config
import org.apache.spark.opentsdb.OpenTSDBContext
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/**
  *
  * @param defaultLimit
  * @param defaultChunkSize
  */
class PhysicalDatasetController(
                                 sparkSession: SparkSession,
                                 kuduMaster: String,
                                 keytab: Option[String] = None,
                                 principal: Option[String] = None,
                                 keytabLocalTempDir: Option[String] = None,
                                 saltwidth: Option[Int] = None,
                                 saltbucket: Option[Int] = None,
                                 override  val defaultLimit: Int = 100,
                                 defaultChunkSize: Int = 0
                               ) extends DatasetOperations {

  lazy val openTSDB = new OpenTSDBController(sparkSession, keytab, principal, keytabLocalTempDir, saltwidth, saltbucket)
  lazy val kudu = new KuduController(sparkSession, kuduMaster)
  lazy val hdfs = new HDFSController(sparkSession)
  val alogger: Logger = LoggerFactory.getLogger(this.getClass)


  private def toTry[T](option: Option[Try[DataFrame]], errorMessage: String): Try[DataFrame] = option match {
    case Some(d) => d
    case None => Failure(new IllegalArgumentException(errorMessage))
  }

  def get(params: Map[String, String]): Try[DataFrame] = {


    val l : Int= params.get("limit").map(_.asInstanceOf[Int]).getOrElse(defaultLimit)
    val limit =
      if(l > defaultLimit)
        defaultLimit
      else l

    params.get("protocol").map(_.asInstanceOf[String]).getOrElse("hdfs") match {

      case "opentsdb" =>
        val metricOp = params.get("metric").map(_.asInstanceOf[String])
        val tags = params.get("tags").map(_.asInstanceOf[Map[String, String]]).getOrElse(Map.empty[String, String])
        val intervalOp = params.get("interval").map(_.asInstanceOf[(Long, Long)])
        alogger.info(s"Reading request for opentsdb with params: metric:$metricOp tags:$tags, interval: $intervalOp")

        val res = metricOp.map(openTSDB.readData(_, tags, intervalOp).map(_.limit(limit)))
        toTry(res, "Metric should be defined")

      case "kudu" =>

        val tableOp = params.get("metric")
        alogger.info(s"Reading request for kudu with params: table:$tableOp")

        val res = tableOp.map(kudu.readData(_).map(_.limit(limit)))
        toTry(res, "Table should be defined")

      case "hdfs" =>

        val format = params.getOrElse("format", "parquet")
        val pathOp = params.get("path")
        alogger.info(s"Reading request for hdfs with params: path:$pathOp format: $format")

        val res = pathOp.map(hdfs.readData(_, format).map(_.limit(limit)))
        toTry(res, "Path should be defined")


      case other =>
        alogger.info(s"Reading request for $other is still not supported")
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

  val alogger: Logger = LoggerFactory.getLogger(this.getClass)

  def apply(configuration: Config): PhysicalDatasetController = {

    val defaultLimit = configuration.getInt("daf.limit_row")

    //private val defaultChunkSize = configuration.getInt("chunk_size").getOrElse(throw new Exception("it shouldn't happen"))

    val sparkConfig = new SparkConf()
    sparkConfig.set("spark.driver.memory", configuration.getString("spark_driver_memory"))

    val sparkSession = SparkSession.builder().master("local").config(sparkConfig).getOrCreate()

    val kuduMaster = configuration.getString("kudu.master")

    System.setProperty("sun.security.krb5.debug", "true")


    val keytab: Option[String] = getOptionalString("opentsdb.context.keytab", configuration)
    alogger.info(s"OpenTSDBContext Keytab: $keytab")

    val principal: Option[String] = getOptionalString("opentsdb.context.principal", configuration)
    alogger.info(s"OpenTSDBContext Principal: $principal")

    val keytabLocalTempDir: Option[String] = getOptionalString("opentsdb.context.keytablocaltempdir", configuration)
    alogger.info(s"OpenTSDBContext Keytab Local Temp Dir: $keytabLocalTempDir")

    val saltwidth: Option[Int] = getOptionalInt("opentsdb.context.saltwidth", configuration)
    alogger.info(s"OpenTSDBContext SaltWidth: $saltwidth")

    val saltbucket: Option[Int] = getOptionalInt("opentsdb.context.saltbucket", configuration)
    alogger.info(s"OpenTSDBContext SaltBucket: $saltbucket")

    val openTSDBContext: OpenTSDBContext = new OpenTSDBContext(sparkSession)
    keytabLocalTempDir.foreach(openTSDBContext.keytabLocalTempDir = _)
    keytab.foreach(openTSDBContext.keytab = _)
    principal.foreach(openTSDBContext.principal = _)
    saltwidth.foreach(OpenTSDBContext.saltWidth = _)
    saltbucket.foreach(OpenTSDBContext.saltBuckets = _)

    new PhysicalDatasetController(
      sparkSession,
      kuduMaster,
      keytab,
      principal,
      keytabLocalTempDir,
      saltwidth,
      saltbucket)

  }
}