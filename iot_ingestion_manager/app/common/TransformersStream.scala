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
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent

import scala.reflect.ClassTag
import scala.util.Try

object TransformersStream {


  def getTransformersStream[B: ClassTag](
                                          ssc: StreamingContext,
                                          topics: Set[String],
                                          kafkaParams: Map[String, AnyRef],
                                          transform: Kleisli[Try, Array[Byte], B]
                                        ): DStream[B] = {
    val inputStream = KafkaUtils.createDirectStream(ssc, PreferConsistent, Subscribe[Array[Byte], Array[Byte]](topics, kafkaParams))
    inputStream.flatMap(cr => {
      val dp = transform(cr.value)
      dp.toOption
    })

  }

}
