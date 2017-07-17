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

import cats.data.Kleisli
import org.apache.hadoop.hbase.client.Table
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils}
import org.apache.spark.streaming.{StreamingContext, Time => SparkTime}
import play.Logger

import scala.reflect.ClassTag
import scala.util.Try

object TransformersStream extends OffsetsManagement {

  private def commitOffsets[A, B](table: Option[Table], topic: String, groupId: String, rdd: RDD[ConsumerRecord[A, B]], time: SparkTime): RDD[ConsumerRecord[A, B]] = rdd match {
    case hasRanges: HasOffsetRanges => setOffsets(table, topic, groupId, hasRanges, time); rdd
    case other => other
  }

  private def stageOffsets[A, B](table: Option[Table], topic: String, groupId: String)(stream: Try[DStream[ConsumerRecord[A, B]]])(implicit A: ClassTag[A]) = stream.map(_.transform((rdd, time) => commitOffsets(table, topic, groupId, rdd, time)))

  def getTransformersStream[B: ClassTag](
                                          ssc: StreamingContext,
                                          kafkaZkQuorum: String,
                                          kafkaZkRootDir: Option[String],
                                          table: Option[Table],
                                          brokers: String,
                                          topic: String,
                                          groupId: String,
                                          transform: Kleisli[Try, Array[Byte], B]
                                        ): Try[DStream[B]] = {

    val kafkaParams = Map[String, AnyRef](
      "bootstrap.servers" -> brokers,
      "key.deserializer" -> classOf[ByteArrayDeserializer],
      "value.deserializer" -> classOf[ByteArrayDeserializer],
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean),
      "group.id" -> groupId
    )

    val fromOffsets = getLastCommittedOffsets(table, topic, groupId, kafkaZkQuorum, kafkaZkRootDir, 4000, 4000)

    fromOffsets.foreach(fo => Logger.info(s"Initial Offsets: $fo"))

    val inputStream = stageOffsets[Array[Byte], Array[Byte]](table, topic, groupId) {
      fromOffsets.map(fromOffsets => KafkaUtils.createDirectStream(ssc, PreferConsistent, Assign[Array[Byte], Array[Byte]](fromOffsets.keys, kafkaParams, fromOffsets)))
    }
    inputStream.map(_.flatMap(cr => {
      val dp = transform(cr.value)
      dp.toOption
    }))
  }

}
