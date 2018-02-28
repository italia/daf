package it.teamdigitale.config

import com.typesafe.config.Config
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.logging.log4j.LogManager


/**
  * This class handle all configurations.
  * Configuration can be classified in three groups: KafkaConfig; KuduConfig and HdfsConfig.
  *
  */
object IotIngestionManagerConfig {

  import RichConfig._

  implicit private val alogger = LogManager.getLogger(this.getClass)

  case class KafkaConfig(brokers: String, groupId: String, topic: String) {

    def getParams() = {
      Map[String, AnyRef](
        "bootstrap.servers" -> brokers,
        "key.deserializer" -> classOf[ByteArrayDeserializer],
        "value.deserializer" -> classOf[ByteArrayDeserializer],
        "auto.offset.reset" -> "earliest",
        "enable.auto.commit" -> (false: java.lang.Boolean),
        "group.id" -> groupId
      )
    }
  }

  case class KuduConfig(masterAdresses: String, eventsTableName: String, eventsNumberBuckets: Int)

  case class HdfsConfig(filename: String)

  def getKuduConfig(configuration: Config): KuduConfig = {

    val masterAddresses = configuration.getStringOrException("kudu.master.addresses")
    val table = configuration.getStringOrException("kudu.events.table.name")
    val buckets = configuration.getStringOrException("kudu.events.table.numberOfBuckets").toInt

    KuduConfig(masterAddresses, table, buckets)

  }

  def getKafkaConfig(configuration: Config): KafkaConfig = {
    val brokers = configuration.getStringOrException("kafka.bootstrap.servers")
    alogger.info(s"Brokers: $brokers")

    val groupId = configuration.getStringOrException("kafka.group.id")
    alogger.info(s"GroupdId: $groupId")

    val topic = configuration.getStringOrException("kafka.topic")
    alogger.info(s"Topics: $topic")

    KafkaConfig(brokers, groupId, topic)

  }

  def getHdfSConfig(configuration: Config): HdfsConfig = {
    val filename = configuration.getStringOrException("hdfs.path")
    alogger.info(s"HDFS path: $filename")
    HdfsConfig(filename)
  }


}
