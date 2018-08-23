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

package config

import java.util.Properties

import it.gov.daf.common.config.Read

import scala.concurrent.duration._

case class KafkaTopicConfig(numPartitions: Int,
                            replicationFactor: Int,
                            cleanupPolicy: String,
                            compressionType: String,
                            maxMessageBytes: Int,
                            retentionTime: FiniteDuration) {

  lazy val props = {
    val properties = new Properties()
    properties.put("cleanup.policy",      cleanupPolicy)
    properties.put("compression.type",    compressionType)
    properties.put("max.message.bytes",   Int.box(maxMessageBytes))
    properties.put("retention.ms",        Long.box(retentionTime.toMillis))
    properties
  }

}

object KafkaTopicConfig {

  private val readConfig = Read.config { "topic" }.!

  private val readValues = for {
    numPartitions     <- Read.int    { "partitions"         }.!
    replicationFactor <- Read.int    { "replication_factor" } default 1
    cleanupPolicy     <- Read.string { "cleanup_policy"     } default "delete"
    compressionType   <- Read.string { "compression_type"   } default "uncompressed"
    maxMessageBytes   <- Read.int    { "max_message_bytes"  } default 1048576
    retentionTime     <- Read.time   { "retention_time"     } default 7.days
  } yield KafkaTopicConfig(
    numPartitions     = numPartitions,
    replicationFactor = replicationFactor,
    cleanupPolicy     = cleanupPolicy,
    compressionType   = compressionType,
    maxMessageBytes   = maxMessageBytes,
    retentionTime     = retentionTime
  )

  val reader = readConfig ~> readValues

}
