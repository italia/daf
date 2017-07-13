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
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils}
import org.apache.spark.streaming.{StreamingContext, Time => SparkTime}

import scala.reflect.ClassTag
import scala.util.Try

object TransformersStream {

  private def setOffsets(hasRanges: HasOffsetRanges, time: SparkTime): Unit = {
    hasRanges.offsetRanges.foreach(println(_))
  }

  private def commitOffsets[A, B](rdd: RDD[ConsumerRecord[A, B]], time: SparkTime): RDD[ConsumerRecord[A, B]] = rdd match {
    case hasRanges: HasOffsetRanges => setOffsets(hasRanges, time); rdd
    case other => other
  }

  private def stageOffsets[A, B](stream: DStream[ConsumerRecord[A, B]])(implicit A: ClassTag[A]) = stream.transform((rdd, time) => commitOffsets(rdd, time))

  def getTransformersStream[B: ClassTag](
                                          ssc: StreamingContext,
                                          topics: Set[String],
                                          kafkaParams: Map[String, AnyRef],
                                          transform: Kleisli[Try, Array[Byte], B]
                                        ): DStream[B] = {
    val inputStream = stageOffsets[Array[Byte], Array[Byte]] {
      KafkaUtils.createDirectStream(ssc, PreferConsistent, Subscribe[Array[Byte], Array[Byte]](topics, kafkaParams))
    }
    inputStream.flatMap(cr => {
      val dp = transform(cr.value)
      dp.toOption
    })
  }

}
