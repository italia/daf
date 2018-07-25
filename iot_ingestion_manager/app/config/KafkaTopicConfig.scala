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

  val reader = for {
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

}
