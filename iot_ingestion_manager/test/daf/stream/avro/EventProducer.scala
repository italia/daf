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

package daf.stream.avro

import java.util.concurrent.TimeUnit

import config.KafkaConfig
import it.gov.daf.iot.event.Event
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.scalatest.{ MustMatchers, WordSpec }

import scala.concurrent.duration._

class EventProducer extends WordSpec with MustMatchers {

  private type Bytes = Array[Byte]

  private val kafkaConfig = KafkaConfig(
    servers      = Seq.empty, //Seq("edge1.novalocal:9092"),
    groupId      = "test-group-1",
    timeout      = 10.seconds,
    offsetReset  = "latest",
    numProducers = 1,
    topicConfig  = null
  )

  private val producer = new KafkaProducer[String, Bytes](kafkaConfig.producerProps(), new StringSerializer, new ByteArraySerializer)

  private def sample(numEvents: Int) = Gen.listOfN(numEvents, AvroGen.event).pureApply(Gen.Parameters.default, Seed.random())

  private def createRecord(topic: String, event: Event) = new ProducerRecord[String, Bytes](
    topic,
    event.getId.toString,
    EventSerDe.serialize(event)
  )

  private def send(numEvents: Int, topic: String) = sample(numEvents).foreach { event =>
    producer.send { createRecord(topic, event) }.get(kafkaConfig.timeout.toMillis, TimeUnit.MILLISECONDS)
  }

  "Event Producer" must {

    "send events" in send(5, "iot-simple-no-null")

  }

}
