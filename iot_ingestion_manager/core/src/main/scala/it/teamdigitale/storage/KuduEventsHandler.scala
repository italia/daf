package it.teamdigitale.storage

import it.teamdigitale.EventModel.StorableEvent
import it.teamdigitale.config.IotIngestionManagerConfig.KuduConfig
import org.apache.kudu.client.{CreateTableOptions, KuduException}
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.kudu.{Schema, Type}
import org.apache.spark.sql.Encoders
import org.slf4j.LoggerFactory

import scala.collection.convert.decorateAsJava._

/**
  * Handles the storage of metric events on KUDU
  */
object KuduEventsHandler {
  implicit private val alogger = LoggerFactory.getLogger(this.getClass)

  val primaryKeys =  List("source", "ts", "id")

  def getOrCreateTable(kuduContext: KuduContext, kuduConfig: KuduConfig): Unit = {
    if (!kuduContext.tableExists(kuduConfig.eventsTableName)) {
      try {
        val schema = Encoders.product[StorableEvent].schema
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
        case ex: KuduException if ex.getStatus.isAlreadyPresent => alogger.error(ex.getMessage)
        case ex =>
          alogger.error(ex.getMessage)
      }
    }
  }
}
