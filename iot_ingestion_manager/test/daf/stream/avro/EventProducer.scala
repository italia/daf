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

import java.util.Properties
import java.util.concurrent.TimeUnit

import config.KafkaConfig
import it.gov.daf.iot.event.Event
import org.apache.kafka.clients.admin.{ AdminClient, AdminClientConfig, NewTopic }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpec }

import scala.concurrent.duration._
import scala.collection.convert.decorateAsJava._

class EventProducer extends WordSpec with MustMatchers with BeforeAndAfterAll {

  private type Bytes = Array[Byte]

  private val brokers = Seq(
    "edge1.novalocal:9092")
//    "edge1.platform.daf.gov.it:9092",
//    "edge2.platform.daf.gov.it:9092",
//    "edge3.platform.daf.gov.it:9092",
//    "edge5.platform.daf.gov.it:9092")

  private val topic = new NewTopic("iot-simple-no-null", 1, 1)

  private val kafkaConfig = KafkaConfig(
    servers      = brokers, // Seq.empty, //Seq("edge1.novalocal:9092"),
    groupId      = "test-group-1",
    timeout      = 10.seconds,
    offsetReset  = "latest",
    numProducers = 1,
    topicConfig  = null
  )

  private val kafkaAdminConfig = Map(
    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG -> brokers.mkString(","),
    AdminClientConfig.CLIENT_ID_CONFIG         -> "test-admin-client"
  )

  private val kafkaAdminProps = kafkaAdminConfig.foldLeft(new Properties) { (props, prop) =>
    props.setProperty(prop._1, prop._2)
    props
  }

  private lazy val kafkaAdmin = AdminClient.create(kafkaAdminProps)

  private val producer = new KafkaProducer[String, Bytes](kafkaConfig.producerProps(), new StringSerializer, new ByteArraySerializer)

  private def sample(numEvents: Int) = Gen.listOfN(numEvents, AvroGen.event).pureApply(Gen.Parameters.default, Seed.random())

  private def createRecord(topicName: String, event: Event) = new ProducerRecord[String, Bytes](
    topicName,
    event.getId.toString,
    EventSerDe.serialize(event)
  )

  private def send(numEvents: Int, topicName: String) = sample(numEvents).foreach { event =>
    producer.send { createRecord(topicName, event) }.get(kafkaConfig.timeout.toMillis, TimeUnit.MILLISECONDS)
  }


  override def beforeAll() = {
    kafkaAdmin.createTopics { Seq(topic).asJavaCollection }
  }

  override def afterAll() = {
    kafkaAdmin.close(5, TimeUnit.SECONDS)
  }

  "Event Producer" must {

    "send events" in send(5, topic.name)

  }

}
