package it.teamdigitale
import org.apache.kudu.spark.kudu._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Try}

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
