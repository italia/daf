package it.teamdigitale.storage

import it.teamdigitale.EventModel.{KuduEvent, StorableEvent}
import it.teamdigitale.config.IotIngestionManagerConfig.KuduConfig
import org.apache.kudu.client.{CreateTableOptions, KuduException}
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.kudu.{Schema, Type}
import org.apache.spark.sql.{DataFrame, Encoders}
import org.apache.logging.log4j.LogManager

import scala.collection.convert.decorateAsJava._

/**
  * Handles the storage of metric events on KUDU
  */
object KuduEventsHandler {
  implicit private val alogger = LogManager.getLogger(this.getClass)

  val primaryKeys =  List("source", "ts", "metric_id")

  def getOrCreateTable(kuduContext: KuduContext, kuduConfig: KuduConfig): Unit = {
    if (!kuduContext.tableExists(kuduConfig.eventsTableName)) {
      try {
        val schema = Encoders.product[KuduEvent].schema
        val table = kuduContext.createTable(
          kuduConfig.eventsTableName,
          schema,
          primaryKeys,
          new CreateTableOptions().
            setRangePartitionColumns(List("ts").asJava)
            .addHashPartitions(List("source").asJava, kuduConfig.eventsNumberBuckets)
        )
        alogger.info(s"Created table ${table.getName}")

      } catch {
        case ex: KuduException if ex.getStatus.isAlreadyPresent => alogger.error(s"Cannot create the table ${kuduConfig.eventsTableName} due the error: ${ex.getMessage}")
        case ex: Throwable => alogger.error(s"Cannot create the table ${kuduConfig.eventsTableName} due the error: ${ex.getMessage}")
      }
    }
  }

  def write(df: DataFrame, kuduContext: KuduContext, kuduConfig: KuduConfig): Unit = {
    kuduContext.insertIgnoreRows(df, kuduConfig.eventsTableName)
  }

}
