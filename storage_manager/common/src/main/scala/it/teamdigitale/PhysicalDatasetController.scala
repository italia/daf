package it.teamdigitale
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


  def toTry[T](option: Option[Try[DataFrame]], errorMessage: String): Try[DataFrame] = option match {
    case Some(d) => d
    case None => Failure(new IllegalArgumentException(errorMessage))
  }

  def get(params: Map[String, Any]): Try[DataFrame] = {


    params.get("protocol").map(_.asInstanceOf[String]).getOrElse("hdfs") match {

      case "opentsdb" =>
        val metricOp = params.get("metric").map(_.asInstanceOf[String])
        val tags = params.get("tags").map(_.asInstanceOf[Map[String, String]]).getOrElse(Map.empty[String, String])
        val intervalOp = params.get("interval").map(_.asInstanceOf[(Long, Long)])
        alogger.info(s"Reading request for opentsdb with params: metric:$metricOp tags:$tags, interval: $intervalOp")

        val res = metricOp.map(openTSDB.readData(_, tags, intervalOp))
        toTry(res, "Metric should be defined")

      case "kudu" =>

        val tableOp = params.get("metric").map(_.asInstanceOf[String])
        alogger.info(s"Reading request for kudu with params: table:$tableOp")

        val res = tableOp.map(kudu.readData)
        toTry(res, "Table should be defined")

      case "hdfs" =>

        val format = params.get("format").map(_.asInstanceOf[String]).getOrElse("parquet")
        val pathOp = params.get("path").map(_.asInstanceOf[String])
        alogger.info(s"Reading request for hdfs with params: path:$pathOp format: $format")

        val res = pathOp.map(hdfs.readData(_, format))
        toTry(res, "Path should be defined")


      case other =>
        alogger.info(s"Reading request for $other is still not supported")
        Failure(new IllegalArgumentException(s"$other is still not supported."))

    }

  }
}

  object PhysicalDatasetController {

    val masterUrl = "local[*]"

    val sparkConfig = new SparkConf()

    sparkConfig.set("spark.driver.memory", "128M")

    val alogger: Logger = LoggerFactory.getLogger(this.getClass)

    //FIXME adding all configuration
    //def apply() = new PhysicalDatasetController()

  }
