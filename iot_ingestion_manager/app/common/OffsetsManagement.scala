package common

import kafka.utils.ZkUtils
import org.apache.hadoop.hbase.client.{Put, Scan, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.common.TopicPartition
import org.apache.spark.streaming.kafka010.HasOffsetRanges
import org.apache.spark.streaming.{Time => SparkTime}
import play.Logger

import scala.util.Try

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Var",
    "org.wartremover.warts.MutableDataStructures",
    "org.wartremover.warts.OptionPartial"
  )
)
trait OffsetsManagement {

  protected def setOffsets(table: Option[Table], topic: String, groupId: String, hasRanges: HasOffsetRanges, time: SparkTime): Unit = {
    table.foreach {
      table =>
        val rowKey = s"$topic:$groupId:${time.milliseconds}"
        val put = new Put(rowKey.getBytes)
        for (offset <- hasRanges.offsetRanges) {
          put.addColumn(
            Bytes.toBytes("offsets"),
            Bytes.toBytes(s"${offset.partition}"),
            Bytes.toBytes(s"${offset.untilOffset}")
          )
        }
        if (hasRanges.offsetRanges.length > 0) {
          table.put(put)
          Logger.info(s"Saved Offsets: ${hasRanges.offsetRanges.map(o => (o.partition, o.untilOffset)).mkString(",")}")
        }
    }
  }

  /**
    * Returns last committed offsets for all the partitions of a given topic from HBase in following cases.
    *- CASE 1: SparkStreaming job is started for the first time. This function gets the number of topic partitions from
    * Zookeeper and for each partition returns the last committed offset as 0
    *- CASE 2: SparkStreaming is restarted and there are no changes to the number of partitions in a topic. Last
    * committed offsets for each topic-partition is returned as is from HBase.
    *- CASE 3: SparkStreaming is restarted and the number of partitions in a topic increased. For old partitions, last
    * committed offsets for each topic-partition is returned as is from HBase as is. For newly added partitions,
    * function returns last committed offsets as 0
    */
  def getLastCommittedOffsets(table: Option[Table], topic: String, groupId: String, zkQuorum: String,
                              zkRootDir: Option[String], sessionTimeout: Int, connectionTimeOut: Int): Try[Map[TopicPartition, Long]] = Try {

    val zkUrl = zkRootDir.fold(zkQuorum)(root => s"$zkQuorum/$root")
    val zkClientAndConnection = ZkUtils.createZkClientAndConnection(zkUrl, sessionTimeout, connectionTimeOut)
    val zkUtils = new ZkUtils(zkClientAndConnection._1, zkClientAndConnection._2, false)
    val zKNumberOfPartitionsForTopic = zkUtils.getPartitionsForTopics(Seq(topic))(topic).size

    //Connect to HBase to retrieve last committed offsets
    val startRow = s"$topic:$groupId:${String.valueOf(System.currentTimeMillis())}"
    val stopRow = s"$topic:$groupId:0"
    val scan = new Scan()
    val scanner = table.get.getScanner(scan.setStartRow(startRow.getBytes).setStopRow(stopRow.getBytes).setReversed(true))
    val result = scanner.next()

    var hbaseNumberOfPartitionsForTopic = 0 //Set the number of partitions discovered for a topic in HBase to 0
    if (result != null) {
      //If the result from hbase scanner is not null, set number of partitions from hbase to the number of cells
      hbaseNumberOfPartitionsForTopic = result.listCells().size()
    }

    val fromOffsets = collection.mutable.Map[TopicPartition, Long]()

    if (hbaseNumberOfPartitionsForTopic == 0) {
      // initialize fromOffsets to beginning
      for (partition <- 0 until zKNumberOfPartitionsForTopic) {
        fromOffsets += (new TopicPartition(topic, partition) -> 0)
      }
    } else if (zKNumberOfPartitionsForTopic > hbaseNumberOfPartitionsForTopic) {
      // handle scenario where new partitions have been added to existing kafka topic
      for (partition <- 0 until hbaseNumberOfPartitionsForTopic) {
        val fromOffset = Bytes.toString(result.getValue(Bytes.toBytes("offsets"), Bytes.toBytes(s"$partition")))
        fromOffsets += (new TopicPartition(topic, partition) -> fromOffset.toLong)
      }
      for (partition <- hbaseNumberOfPartitionsForTopic until zKNumberOfPartitionsForTopic) {
        fromOffsets += (new TopicPartition(topic, partition) -> 0)
      }
    } else {
      //initialize fromOffsets from last run
      for (partition <- 0 until hbaseNumberOfPartitionsForTopic) {
        val fromOffset = Bytes.toString(result.getValue(Bytes.toBytes("offsets"), Bytes.toBytes(s"$partition")))
        fromOffsets += (new TopicPartition(topic, partition) -> fromOffset.toLong)
      }
    }
    scanner.close()
    fromOffsets.toMap
  }

}
