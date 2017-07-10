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

import it.gov.teamdigitale.iotingestion.common.SerializerDeserializer
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import it.gov.teamdigitale.iotingestion.event.Event

import scala.util.Try

abstract class IoTInputStreamManager(
                    @transient val ssc: StreamingContext,
                    topics: Set[String],
                    kafkaParams: Map[String, AnyRef]
                 ) extends Serializable {

  def getStream(): DStream[Event] = {

    val inputStream = KafkaUtils.createDirectStream(ssc, PreferConsistent, Subscribe[Array[Byte], Array[Byte]](topics, kafkaParams))
    inputStream.mapPartitions { rdd =>
      rdd.flatMap { el =>
        val data = SerializerDeserializer.deserialize(el.value())
        data.toOption
      }
    }
  }

  def write(inputStream: DStream[Event]): Try[Unit]

}

class OpenTSDBStreamManager(
                              ssc: StreamingContext,
                              topics: Set[String],
                              kafkaParams: Map[String, AnyRef]
                           ) extends IoTInputStreamManager(ssc, topics, kafkaParams) {

  def write(inputStream: DStream[Event]): Try[Unit] = ???


}
