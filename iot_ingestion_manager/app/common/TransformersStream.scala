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
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kudu.client.KuduClient
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Encoder, SparkSession}
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils}
import org.apache.spark.streaming.{StreamingContext, Time => SparkTime}
import play.Logger

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.ImplicitParameter"
  )
)
object TransformersStream extends OffsetsManagement {

  @transient
  implicit private val alogger = Logger.of(this.getClass.getCanonicalName)

  private def commitOffsets[A, B](kuduClient: KuduClient, tableName: String, topic: String, groupId: String, rdd: RDD[ConsumerRecord[A, B]], time: SparkTime): RDD[ConsumerRecord[A, B]] = rdd match {
    case hasRanges: HasOffsetRanges => setOffsets(kuduClient, tableName, topic, groupId, hasRanges, time); rdd
    case other => other
  }

  /**
    * It uses the transform method for managing the kafka offsets, for each stream rdd the offsets are first saved then it's passed to the next stage.
    *
    * @param kuduClient
    * @param tableName
    * @param topic
    * @param groupId
    * @param stream
    * @param A
    * @tparam A
    * @tparam B
    * @return
    */
  private def stageOffsets[A, B](kuduClient: KuduClient, tableName: String, topic: String, groupId: String)(stream: Try[DStream[ConsumerRecord[A, B]]])(implicit A: ClassTag[A]) = stream.map(_.transform((rdd, time) => commitOffsets(kuduClient, tableName, topic, groupId, rdd, time)))


  /**
    * It creates a kafka direct stream where the kafka offsets are managed.
    *
    * @param ssc
    * @param kuduContext
    * @param kafkaZkQuorum
    * @param kafkaZkRootDir
    * @param tableName
    * @param brokers
    * @param topic
    * @param groupId
    * @param transform
    * @tparam B
    * @return a stream
    */
  def getTransformersStream[B: ClassTag](
                                          ssc: StreamingContext,
                                          kuduContext: KuduContext,
                                          kafkaZkQuorum: String,
                                          kafkaZkRootDir: Option[String],
                                          tableName: String,
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

    val fromOffsets = getLastCommittedOffsets(kuduContext.syncClient, tableName, topic, groupId, kafkaZkQuorum, kafkaZkRootDir, 60000, 60000) //TODO Magic numbers

//    val inputStream: Try[DStream[ConsumerRecord[Array[Byte], Array[Byte]]]] = stageOffsets[Array[Byte], Array[Byte]](kuduContext.syncClient, tableName, topic, groupId) {
//      fromOffsets.map(fromOffsets => KafkaUtils.createDirectStream(ssc, PreferConsistent, Assign[Array[Byte], Array[Byte]](fromOffsets.keys, kafkaParams, fromOffsets)))
//    }

    val inputStream = Try(KafkaUtils.createDirectStream(ssc, PreferConsistent, Subscribe[Array[Byte], Array[Byte]](Set(topic), kafkaParams)))

    inputStream.map(_.flatMap{cr =>
      val dp  = transform(cr.value)
      logInfo(dp)
      dp.toOption
    })
  }

  implicit class EnrichedDStream[A](dstream: DStream[A]) extends AnyRef {
    def applyTransform[B: ClassTag](transform: Kleisli[Try, A, B]) = dstream.flatMap(e => {
      val dp = transform(e)
      logInfo(dp)
      dp.toOption
    })
  }


  def convertDataFrameToRDD[T <: Product](data: DataFrame)(implicit encoder: Encoder[T]) = {
    data.as[T].rdd
  }

  def convertRDDtoDataFrame[T <: Product](data: RDD[T])(implicit sparkSession: SparkSession, encoder: Encoder[T]): DataFrame = {
    import sparkSession.implicits._
    data.toDS.toDF
  }
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private def logInfo[B: ClassTag](obj: Try[B]): Unit = {
    obj match {
      case Failure(ex) =>
        alogger.error(s"${ex.getMessage}")
      case Success(d) =>  alogger.debug(s"${d.toString} correctly transformed")
    }
  }

}
