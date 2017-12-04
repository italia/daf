package it.teamdigitale

import org.apache.spark.opentsdb.OpenTSDBContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Try}

class OpenTSDBController(sparkSession: SparkSession,
                         keytab: Option[String] = None,
                         principal: Option[String] = None,
                         keytabLocalTempDir: Option[String] = None,
                         saltwidth:Option[Int] = None,
                         saltbucket: Option[Int] = None
                        ) {

  val openTSDBContext : OpenTSDBContext = new OpenTSDBContext(sparkSession)
  keytabLocalTempDir.foreach(openTSDBContext.keytabLocalTempDir = _)
  keytab.foreach(openTSDBContext.keytab = _)
  principal.foreach(openTSDBContext.principal = _)

  //num bit per definire la chiave 1 va da 0 a 255
  saltwidth.foreach(OpenTSDBContext.saltWidth = _)
  saltbucket.foreach(OpenTSDBContext.saltBuckets = _)

  val alogger: Logger = LoggerFactory.getLogger(this.getClass)


  def readData(metric: String, tags: Map[String, String] = Map.empty[String, String], interval: Option[(Long, Long)] = None): Try[DataFrame] = {
    alogger.info(s"Reading Data metric: <$metric> tags: $tags inteval: $interval")
    Try(openTSDBContext.loadDataFrame(metric, tags, interval)) recoverWith {
      case ex =>
        alogger.error(s"Exception ${ex.getMessage}\n ${ex.getStackTrace.mkString("\n")} ")
        Failure(ex)
    }

  }

}
