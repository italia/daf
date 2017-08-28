/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package common

import kafka.utils.ZkUtils
import org.apache.kafka.common.TopicPartition
import org.apache.kudu.client.KuduPredicate._
import org.apache.kudu.client.{KuduClient, RowResult}
import org.apache.spark.streaming.kafka010.HasOffsetRanges
import org.apache.spark.streaming.{Time => SparkTime}
import play.Logger

import scala.collection.convert.decorateAsJava._
import scala.collection.convert.decorateAsScala._
import scala.util.Try

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Var",
    "org.wartremover.warts.MutableDataStructures"
  )
)
trait OffsetsManagement {

  private val alogger = Logger.of(this.getClass.getCanonicalName)

  protected def setOffsets(kuduClient: KuduClient, tableName: String, topic: String, groupId: String, hasRanges: HasOffsetRanges, time: SparkTime): Unit = {
    val table = kuduClient.openTable(tableName)
    if (hasRanges.offsetRanges.length > 0) {
      val session = kuduClient.newSession()
      val upsert = table.newUpsert()
      val row = upsert.getRow
      row.addString(0, topic)
      row.addString(1, groupId)
      val offsetsRanges: Array[(Int, Long)] = hasRanges.offsetRanges.map(range => (range.partition, range.untilOffset))
      row.addString(2, offsetsRanges.map(_._1).mkString(","))
      row.addString(3, offsetsRanges.map(_._2).mkString(","))
      val res1 = session.apply(upsert)
      val res2 = session.close()
      alogger.info(s"Saved Offsets: ${hasRanges.offsetRanges.map(o => (o.partition, o.untilOffset)).mkString(",")}")
    }
  }

  /**
    * Returns last committed offsets for all the partitions of a given topic from Kudu in following cases.
    *- CASE 1: SparkStreaming job is started for the first time. This function gets the number of topic partitions from
    * Zookeeper and for each partition returns the last committed offset as 0
    *- CASE 2: SparkStreaming is restarted and there are no changes to the number of partitions in a topic. Last
    * committed offsets for each topic-partition is returned as is from Kudu.
    *- CASE 3: SparkStreaming is restarted and the number of partitions in a topic increased. For old partitions, last
    * committed offsets for each topic-partition is returned as is from Kudu as is. For newly added partitions,
    * function returns last committed offsets as 0
    */
  def getLastCommittedOffsets(kuduClient: KuduClient, tableName: String, topic: String, groupId: String, zkQuorum: String,
                              zkRootDir: Option[String], sessionTimeout: Int, connectionTimeOut: Int): Try[Map[TopicPartition, Long]] = Try {

    val zkUrl = zkRootDir.fold(zkQuorum)(root => s"$zkQuorum/$root")
    val zkClientAndConnection = ZkUtils.createZkClientAndConnection(zkUrl, sessionTimeout, connectionTimeOut)
    val zkUtils = new ZkUtils(zkClientAndConnection._1, zkClientAndConnection._2, false)
    val zKNumberOfPartitionsForTopic = zkUtils.getPartitionsForTopics(Seq(topic))(topic).size

    //Connect to Kudu to retrieve last committed offsets
    val kuduTable = kuduClient.openTable(tableName)
    val projectColumns = List("partitions", "offsets")
    val scanner = kuduClient.newScannerBuilder(kuduTable).
      setProjectedColumnNames(projectColumns.asJava).
      addPredicate(newComparisonPredicate(kuduTable.getSchema.getColumn("topic"), ComparisonOp.EQUAL, topic)).
      addPredicate(newComparisonPredicate(kuduTable.getSchema.getColumn("groupId"), ComparisonOp.EQUAL, groupId)).
      build
    val rows = Option(scanner.nextRows())

    //Connect to Kudu to retrieve last committed offsets
    var kuduNumberOfPartitionsForTopic = 0
    var result: Option[RowResult] = None
    rows.fold()(rows => {
      result = rows.iterator().asScala.toList.headOption
      result.foreach(rowResult => {
        val partitions = rowResult.getString(0)
        val offsets = rowResult.getString(1)
        kuduNumberOfPartitionsForTopic = partitions.split(",").length
      })
    })

    val fromOffsets = collection.mutable.Map[TopicPartition, Long]()

    if (kuduNumberOfPartitionsForTopic == 0) {
      // initialize fromOffsets to beginning
      for (partition <- 0 until zKNumberOfPartitionsForTopic) {
        fromOffsets += (new TopicPartition(topic, partition) -> 0)
      }
    } else if (zKNumberOfPartitionsForTopic > kuduNumberOfPartitionsForTopic) {
      // handle scenario where new partitions have been added to existing kafka topic
      result foreach {
        result =>
          val offsets = result.getString(1).split(",")
          for (partition <- 0 until kuduNumberOfPartitionsForTopic) {
            val fromOffset = offsets(partition)
            fromOffsets += (new TopicPartition(topic, partition) -> fromOffset.toLong)
          }
          for (partition <- kuduNumberOfPartitionsForTopic until zKNumberOfPartitionsForTopic) {
            fromOffsets += (new TopicPartition(topic, partition) -> 0)
          }
      }
    } else {
      //initialize fromOffsets from last run
      result foreach {
        result =>
          val offsets = result.getString(1).split(",")
          for (partition <- 0 until kuduNumberOfPartitionsForTopic) {
            val fromOffset = offsets(partition)
            fromOffsets += (new TopicPartition(topic, partition) -> fromOffset.toLong)
          }
      }
    }
    val _ = scanner.close()

    fromOffsets.foreach(fo => alogger.info(s"Initial Offsets: $fo"))

    fromOffsets.toMap
  }

}
