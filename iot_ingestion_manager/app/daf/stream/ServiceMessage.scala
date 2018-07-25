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

package daf.stream

import org.apache.kafka.clients.producer.RecordMetadata

final case class Sender(source: String, userId: String)

sealed trait EventType

case object MetricEvent extends EventType
case object StateChangeEvent extends EventType
case object OtherEvent extends EventType

final case class Location(latitude: Double, longitude: Double)

final case class Envelope(id: String,
                          sender: Sender,
                          topic: String,
                          version: Long,
                          timestamp: Long,
                          certainty: Double,
                          eventType: EventType,
                          subType: Option[String],
                          comment: Option[String],
                          location: Option[Location])

final case class ServiceMessage(envelope: Envelope,
                                payload: Map[String, Any],
                                attributes: Map[String, Any] = Map.empty[String, Any])

final case class ServiceMessageMetadata(id: String, topic: String, offset: Long, partition: Int, timestamp: Long)

object ServiceMessageMetadata {

  def fromKafka(id: String, kafkaMetadata: RecordMetadata): ServiceMessageMetadata = apply(
    id        = id,
    topic     = kafkaMetadata.topic(),
    offset    = kafkaMetadata.offset(),
    partition = kafkaMetadata.partition(),
    timestamp = kafkaMetadata.timestamp()
  )

}
