package it.teamdigitale.config

import com.typesafe.config.Config
import it.teamdigitale.managers.IotIngestionManager
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.slf4j.LoggerFactory

/**
  * This class handle all configurations.
  * Configuration can be classified in three groups: KafkaConfig; KuduConfig and HdfsConfig.
  *
  */
object IotIngestionManagerConfig {

  import RichConfig._

  implicit private val alogger = LoggerFactory.getLogger(this.getClass)

  //case class KafkaConfig(brokers: String, groupId: String, topic: String, kafkaZkQuorum: String, kafkaZkRootDir: Option[String]) {
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

    //val offsetConfig = getKuduOffsetTableConfig(configuration)

    KuduConfig(masterAddresses, table, buckets)

  }

  def getKafkaConfig(configuration: Config): KafkaConfig = {
    val brokers = configuration.getStringOrException("bootstrap.servers")
    alogger.info(s"Brokers: $brokers")

    val groupId = configuration.getStringOrException("group.id")
    alogger.info(s"GroupdId: $groupId")

    val topic = configuration.getStringOrException("topic")
    alogger.info(s"Topics: $topic")

//    val kafkaZkQuorum = configuration.getStringOrException("kafka.zookeeper.quorum")
//    alogger.info(s"Kafka Zk Quorum: $kafkaZkQuorum")
//
//    val kafkaZkRootDir = configuration.getOptionalString("kafka.zookeeper.root")
//    alogger.info(s"Kafka Zk Root Dir: $kafkaZkRootDir")

    KafkaConfig(brokers, groupId, topic)

  }

  def getHdfSConfig(configuration: Config): HdfsConfig = {
    val filename = configuration.getStringOrException("hdfs.filename")
    alogger.info(s"HDFS filename: $filename")
    HdfsConfig(filename)
  }


}
