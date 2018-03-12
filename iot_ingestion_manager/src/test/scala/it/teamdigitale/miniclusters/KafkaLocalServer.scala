package it.teamdigitale.miniclusters

import java.io.File
import java.net.InetSocketAddress
import java.util.Properties

import it.gov.daf.iotingestion.event.Event
import it.teamdigitale.events.SerializerDeserializer
import kafka.admin.AdminUtils
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.commons.io.FileUtils
import org.apache.zookeeper.server.{ServerCnxnFactory, ZooKeeperServer}
import org.apache.logging.log4j.LogManager

class KafkaLocalServer  extends AutoCloseable {
  var zkServer: Option[ServerCnxnFactory] = None
  var kafkaServer: Option[KafkaServer] = None
  val logger = LogManager.getLogger(this.getClass)

  private val ip = "127.0.0.1"
  private val port = "9092"
  private val broker = "localhost"


  private def startZK(): Unit = {
    if (zkServer.isEmpty) {

      val dataDirectory = System.getProperty("java.io.tmpdir")
      val dir = new File(dataDirectory, "zookeeper")
      println(dir.toString)
      if (dir.exists())
        FileUtils.deleteDirectory(dir)

      try {
        val tickTime = 10000
        val server = new ZooKeeperServer(dir.getAbsoluteFile, dir.getAbsoluteFile, tickTime)
        val factory = ServerCnxnFactory.createFactory
        factory.configure(new InetSocketAddress("0.0.0.0", 2181), 1024)
        factory.startup(server)
        logger.info("ZOOKEEPER server up!!")
        zkServer = Some(factory)

      } catch {
        case ex: Exception => System.err.println(s"Error in zookeeper server: ${ex.printStackTrace()}")
      } finally { dir.deleteOnExit() }
    } else logger.info("ZOOKEEPER is already up")
  }

  private def stopZK() = {
    if (zkServer.isDefined) {
      zkServer.get.shutdown()
    }
    logger.info("ZOOKEEPER server stopped")
  }

  private def startKafka() = {
    if (kafkaServer.isEmpty) {
      val dataDirectory = System.getProperty("java.io.tmpdir")
      val dir = new File(dataDirectory, "kafka")
      if (dir.exists())
        FileUtils.deleteDirectory(dir)
      try {
        val props = new Properties()
        //props.setProperty("hostname", "localhost")
        props.setProperty("port", port)
        props.setProperty("broker.id", "0")
        props.setProperty("log.dir", dir.getAbsolutePath)
        props.setProperty("enable.zookeeper", "true")
        props.setProperty("zookeeper.connect", "localhost:2181")
        props.setProperty("advertised.host.name", "localhost")
        props.setProperty("connections.max.idle.ms", "9000000")
        props.setProperty("zookeeper.connection.timeout.ms", "10000")
        props.setProperty("zookeeper.session.timeout.ms", "10000")

        // flush every message.
        props.setProperty("log.flush.interval", "100")

        // flush every 1ms
        props.setProperty("log.default.flush.scheduler.interval.ms", "1000")

        val server = new KafkaServer(new KafkaConfig(props, false))
        server.startup()
        logger.info("KAFKA server on!!")

        kafkaServer = Some(server)
      } catch {
        case ex: Exception => System.err.println(s"Error in kafka server: ${ex.getMessage}")
      } finally {
        dir.deleteOnExit()
      }
    } else logger.info("KAFKA is already up")
  }

  private def stopKafka(): Unit = {
    if (kafkaServer.isDefined) {
      kafkaServer.get.shutdown()

      logger.info(s"KafkaServer ${kafkaServer.get.config.hostName} run state is: ${kafkaServer.get.kafkaController.isActive()} ")
    }
    logger.info("KAFKA server stopped")
  }

  def getKafkaConf() = it.teamdigitale.config.IotIngestionManagerConfig.KafkaConfig(s"$broker:$port", "groupTest", "kafkaTopic")

  def getKafkaProps() = {
    val props: Properties = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$broker:$port")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
    props
  }

  def createTopic(topic: String): Unit = {
    AdminUtils.createTopic(kafkaServer.get.zkUtils, topic, 3, 1, new Properties())
    logger.info(s"Topic $topic created!")
    println(s"Topic $topic created!")
  }

  def start(): Unit = {
    startZK()
    Thread.sleep(5000)
    startKafka()
  }

  override def close(): Unit = {
    stopKafka()
    Thread.sleep(2000)
    stopZK()
  }

}

object KafkaLocalServer{

  def main(args: Array[String]): Unit = {

    try {
      val kafka = new KafkaLocalServer()
      val kafkaConf = kafka.getKafkaConf()
      kafka.start()
      kafka.createTopic(kafkaConf.topic)

      val props = kafka.getKafkaProps()
      val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)

      Range(1, 1000).foreach { i =>

        val event = Event(
          version = 1L,
          id = i + "metric",
          ts = System.currentTimeMillis(),
          event_type_id = 0,
          location = "41.1260529:16.8692905",
          source = "sourceidTest",
          body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
          attributes = Map(
            "tag1" -> "Via Cernaia(TO)",
            "tag2" -> "value2",
            "metric" -> "speed",
            "tags" -> "tag1, tag2")
        )
        val bytes = SerializerDeserializer.serialize(event)
        val message = new ProducerRecord[Array[Byte], Array[Byte]](kafkaConf.topic, bytes)
        producer.send(message)
        producer.flush()
        Thread.sleep(1000)
      }

    }
  }
}
