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

import akka.actor.ActorRefFactory
import akka.pattern._
import akka.routing.RoundRobinPool
import akka.util.Timeout
import config.KafkaConfig
import daf.stream.error.StreamForwardingError
import representation._
import representation.{ Event => PushedEvent }

import scala.util.{ Failure, Success }

class ProducerService(kafkaConfig: KafkaConfig, validator: PayloadValidator)(implicit actorRefFactory: ActorRefFactory) {

  private implicit val askTimeout = Timeout.durationToTimeout { kafkaConfig.timeout }

  private val producerRouter = actorRefFactory.actorOf {
    RoundRobinPool(kafkaConfig.numProducers).props { ProducerActor.props(kafkaConfig) }
  }

  private def extractTopic(streamData: StreamData) = streamData.source match {
    case KafkaSource(topic) => Success(topic)
    case source             => Failure { StreamForwardingError(s"Unrecognized stream setting [$source] - expected [KafkaSource]") }
  }

  private def convertEventType(pushedEvent: PushedEvent) = pushedEvent.eventType match {
    case MetricEventType       => MetricEvent
    case StateChangedEventType => StateChangeEvent
    case OtherEventType        => OtherEvent
  }

  private def convertAttributes(attributes: Map[String, Any]) = attributes.map { case (k, v) => k -> v.toString }

  private def createEnvelope(streamData: StreamData, userId: String, pushedEvent: PushedEvent) = extractTopic(streamData).map { topic =>
    Envelope(
      id = streamData.id,
      sender = Sender(
        source = pushedEvent.source,
        userId = userId
      ),
      topic     = topic,
      version   = pushedEvent.version getOrElse 1l,
      timestamp = pushedEvent.timestamp,
      location  = pushedEvent.location.map { location =>
        Location(
          latitude  = location.latitude,
          longitude = location.longitude
        )
      },
      certainty = pushedEvent.certainty getOrElse 1.0d,
      eventType = convertEventType(pushedEvent),
      subType   = pushedEvent.customType,
      comment   = pushedEvent.comment
    )
  }

  private def createMessage(streamData: StreamData, userId: String, pushedEvent: PushedEvent) = for {
    envelope <- createEnvelope(streamData, userId, pushedEvent)
    payload  <- validator.validate(pushedEvent.payload.value.toMap, streamData)
  } yield ServiceMessage(
    envelope   = envelope,
    payload    = pushedEvent.payload,
    attributes = pushedEvent.attributes
  )

  def sendMessage(streamData: StreamData, userId: String, pushedEvent: PushedEvent) =
    { producerRouter ? createMessage(streamData, userId, pushedEvent) }.mapTo[ServiceMessageMetadata]

}
