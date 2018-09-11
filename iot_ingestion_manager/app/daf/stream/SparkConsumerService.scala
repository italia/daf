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

import java.util.concurrent.TimeUnit

import config.KafkaConfig
import daf.stream.error.StreamCreationError
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.{ DataStreamWriter, StreamingQuery, Trigger }
import org.slf4j.LoggerFactory
import representation._

import scala.concurrent.{ Future, ExecutionContext }
import scala.util.{ Failure, Success, Try }

class SparkConsumerService(val kafkaConfig: KafkaConfig)(implicit val executionContext: ExecutionContext) extends ConsumerService {

  private val logger = LoggerFactory.getLogger("it.gov.daf.SparkConsumer")

  private val sparkSession = SparkSession.builder()
    .master("local")
    .appName("IoT-Ingestion-Manager")
    .config("spark.driver.memory", "256m")
    .config("spark.executor.memory", "512m")
    .getOrCreate()

  def findStream(id: String) = sparkSession.streams.active.toSeq.find { _.name == id }

  private def prepareStream(streamData: StreamData) = streamData.source match {
    case KafkaSource(topic)       => prepareKafka(streamData.id, streamData.interval, topic)
    case SocketSource(host, port) => prepareSocket(streamData.id, streamData.interval, host, port)
  }

  private def startStream[A](streamData: StreamData, stream: DataStreamWriter[A]) = streamData.sink match {
    case ConsoleSink    => Future { stream.format("console").start() }
    case HdfsSink(path) => Future { stream.format("parquet").option("path", path).start() }
    case KuduSink(_)    => Future.failed { new IllegalArgumentException(s"Kudu sink is not supported for stream id [${streamData.id}]") }
  }

  private def prepareSocket(id: String, interval: Long, host: String, port: Int) = Future {
    sparkSession
      .readStream
      .format("socket")
      .option("host", host)
      .option("port", port)
      .load()
      .writeStream
      .queryName(id)
      .trigger { Trigger.ProcessingTime(interval, TimeUnit.SECONDS) }
  }

  private def prepareKafka(id: String, interval: Long, topic: String) = Future {
    sparkSession.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaConfig.servers mkString ",")
      .option("subscribe", topic)
      .load()
      .writeStream
      .queryName(id)
      .trigger { Trigger.ProcessingTime(interval, TimeUnit.SECONDS) }
  }

  private def checkQuery(query: StreamingQuery): Future[Unit] = if (query.isActive) Future.successful {
    logger.info { s"Query with id [${query.id}] :: name [${query.name}] started consuming" }
  } else Future.failed { StreamCreationError(s"Query with id [${query.id}] :: name [${query.name}] failed to start", query.exception.orNull) }

  def createConsumer(streamData: StreamData) = for {
    stream <- prepareStream(streamData)
    query  <- startStream(streamData, stream)
    _      <- checkQuery(query)
  } yield ()

}
