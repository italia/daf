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
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger

import scala.util.Try

class StreamService(kafkaConfig: KafkaConfig) {

  private val sparkSession = SparkSession.builder()
    .master("local")
    .appName("IoT-Ingestion-Manager")
    .config("spark.driver.memory", "256m")
    .config("spark.executor.memory", "512m")
    .getOrCreate()

  def findStream(id: String) = sparkSession.streams.active.toSeq.find { _.name == id }

  def createSocket(id: String, interval: Int, port: Int) = Try {
    sparkSession
      .readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", port)
      .load()
      .writeStream
      .queryName(id)
      .trigger { Trigger.ProcessingTime(interval, TimeUnit.SECONDS) }
      .format("console")
      .start()
  }

  def createKafka(id: String, interval: Int, topic: String) = Try {
    sparkSession.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaConfig.servers mkString ",")
      .option("subscribe", topic)
      .load()
      .writeStream
      .queryName(id)
      .trigger { Trigger.ProcessingTime(interval, TimeUnit.SECONDS) }
      .format("console")
      .start()

  }

}
