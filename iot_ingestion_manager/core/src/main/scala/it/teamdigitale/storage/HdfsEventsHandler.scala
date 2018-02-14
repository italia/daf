package it.teamdigitale.storage

import it.teamdigitale.config.IotIngestionManagerConfig.HdfsConfig
import org.apache.spark.sql.{DataFrame, SaveMode}

/**
  * Handles the storage of generic events on HDFS
 */
object HdfsEventsHandler{

  val partitionList: List[String] = List("source", "day")

  def write(genericEventsDF: DataFrame, hdfsConfig: HdfsConfig): Unit = genericEventsDF.write.partitionBy(partitionList: _*).mode(SaveMode.Append).parquet(hdfsConfig.filename)
}
