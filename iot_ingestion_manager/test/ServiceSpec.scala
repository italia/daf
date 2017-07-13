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

import java.io.{FileNotFoundException, IOException, File => JFile}
import java.net.ServerSocket
import java.util.{Base64, Properties}

import ServiceSpec._
import better.files._
import it.gov.daf.iotingestionmanager.client.Iot_ingestion_managerClient
import it.gov.teamdigitale.daf.iotingestion.common.SerializerDeserializer
import it.gov.teamdigitale.daf.iotingestion.event.Event
import kafka.server.{KafkaConfig, KafkaServer, RunningAsBroker}
import kafka.utils.{MockTime, TestUtils, ZkUtils}
import org.I0Itec.zkclient.ZkClient
import org.I0Itec.zkclient.serialize.ZkSerializer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseTestingUtility, TableName}
import org.apache.hadoop.test.PathUtils
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.opentsdb.OpenTSDBConfigurator
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test.WithServer

import scala.collection.convert.decorateAsScala._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.While"
  )
)
class ServiceSpec extends Specification with BeforeAfterAll {

  private val TIMEOUT = 1000

  private val NUMBROKERS = 1

  private val BROKERHOST = "127.0.0.1"

  private var zkUtils: Try[ZkUtils] = Failure[ZkUtils](new Exception(""))

  private var kafkaServers: Array[Try[KafkaServer]] = new Array[Try[KafkaServer]](NUMBROKERS)

  private var logDir: Try[JFile] = Failure[JFile](new Exception(""))

  private var producer: KafkaProducer[Array[Byte], Array[Byte]] = _

  private def getAvailablePort: Int = {
    try {
      val socket = new ServerSocket(0)
      try {
        socket.getLocalPort
      } finally {
        socket.close()
      }
    } catch {
      case e: IOException =>
        throw new IllegalStateException(s"Cannot find available port: ${e.getMessage}", e)
    }
  }

  private def constructTempDir(dirPrefix: String): Try[JFile] = Try {
    val rndrange = 10000000
    val file = new JFile(System.getProperty("java.io.tmpdir"), s"$dirPrefix${Random.nextInt(rndrange)}")
    if (!file.mkdirs())
      throw new RuntimeException("could not create temp directory: " + file.getAbsolutePath)
    file.deleteOnExit()
    file
  }

  private def deleteDirectory(path: JFile): Boolean = {
    if (!path.exists()) {
      throw new FileNotFoundException(path.getAbsolutePath)
    }
    var ret = true
    if (path.isDirectory)
      path.listFiles().foreach(f => ret = ret && deleteDirectory(f))
    ret && path.delete()
  }

  private def makeZkUtils(zkPort: String): Try[ZkUtils] = Try {
    val zkConnect = s"localhost:$zkPort"
    val zkClient = new ZkClient(zkConnect, Integer.MAX_VALUE, TIMEOUT, new ZkSerializer {
      def serialize(data: Object): Array[Byte] = data.asInstanceOf[String].getBytes("UTF-8")

      def deserialize(bytes: Array[Byte]): Object = new String(bytes, "UTF-8")
    })
    ZkUtils.apply(zkClient, isZkSecurityEnabled = false)
  }

  private def makeKafkaServer(zkConnect: String, brokerId: Int): Try[KafkaServer] = Try {
    logDir = constructTempDir("kafka-local")
    val brokerPort = getAvailablePort
    val brokerProps = new Properties()
    brokerProps.setProperty("zookeeper.connect", zkConnect)
    brokerProps.setProperty("broker.id", s"$brokerId")
    logDir.foreach(f => brokerProps.setProperty("log.dirs", f.getAbsolutePath))
    brokerProps.setProperty("listeners", s"PLAINTEXT://$BROKERHOST:$brokerPort")
    val config = new KafkaConfig(brokerProps)
    val mockTime = new MockTime()
    val server = TestUtils.createServer(config, mockTime)
    while (!(server.brokerState.currentState == RunningAsBroker.state)) {
      Thread.sleep(100)
    }
    server
  }

  private def shutdownKafkaServers(): Unit = {
    kafkaServers.foreach(_.foreach(_.shutdown()))
    kafkaServers.foreach(_.foreach(_.awaitShutdown()))
    logDir.foreach(deleteDirectory)
  }

  private def getBootstrapServers: String = kafkaServers.
    map(tks => s"localhost:${tks.getOrElse(throw new RuntimeException).config.listeners.head._2.port}").
    mkString(",")

  private def application: Application = GuiceApplicationBuilder().
    configure("hadoop_conf_dir" -> ServiceSpec.confPath.pathAsString).
    configure("pac4j.authenticator" -> "test").
    configure("bootstrap.servers" -> getBootstrapServers).
    build()

  "This test" should {
    "pass" in new WithServer(app = application, port = getAvailablePort) {
      val ws: AhcWSClient = AhcWSClient()

      val plainCreds = "david:david"
      val plainCredsBytes = plainCreds.getBytes
      val base64CredsBytes = Base64.getEncoder.encode(plainCredsBytes)
      val base64Creds = new String(base64CredsBytes)
      val client = new Iot_ingestion_managerClient(ws)(s"http://localhost:$port")
      val result1 = Await.result(client.start(s"Basic $base64Creds"), Duration.Inf)

      Thread.sleep(4000)

      for (i <- 1 to 200) {
        val jsonEvent =
          """{"id": "TorinoFDT",
             |"ts": 1488532860000,
             |"event_type_id": 1,
             |"source": "-1965613475",
             |"location": "45.06766-7.66662",
             |"service": "http://opendata.5t.torino.it/get_fdt",
             |"body": {"bytes": "<FDT_data period=\"5\" accuracy=\"100\" lng=\"7.66662\" lat=\"45.06766\" direction=\"positive\" offset=\"55\" Road_name=\"Corso Vinzaglio(TO)\" Road_LCD=\"40201\" lcd1=\"40202\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.5t.torino.it/simone/ns/traffic_data\">\n    <speedflow speed=\"20.84\" flow=\"528.00\"/>\n  </FDT_data>"},
             |"attributes": {"period": "5", "offset": "55", "Road_name": "Corso Vinzaglio(TO)", "Road_LCD": "40201", "accuracy": "100", "FDT_data": "40202", "flow": "528.00", "speed": "20.84", "direction": "positive"}
             |}""".stripMargin

        implicit val formats: DefaultFormats = DefaultFormats
        val event = parse(jsonEvent, true).extract[Event]
        val eventBytes = SerializerDeserializer.serialize(event)
        
        val record = new ProducerRecord[Array[Byte], Array[Byte]]("daf-iot-events", s"$i".getBytes(), eventBytes)
        producer.send(record)
      }

      Thread.sleep(1000)

      val result2 = Await.result(client.stop(s"Basic $base64Creds"), Duration.Inf)
    }
  }

  private val hbaseUtil = new HBaseTestingUtility()

  private var baseConf: Configuration = _

  override def beforeAll(): Unit = {
    hbaseUtil.startMiniCluster(4)
    val conf = new SparkConf().
      setAppName("daf-iot-manager-local-test").
      setMaster("local[4]").
      set("spark.io.compression.codec", "lzf")
    baseConf = hbaseUtil.getConfiguration
    hbaseUtil.createTable(TableName.valueOf("tsdb-uid"), Array("id", "name"))
    hbaseUtil.createTable(TableName.valueOf("tsdb"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-tree"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-meta"), Array("name"))
    val confFile: File = confPath / "hbase-site.xml"
    for {os <- confFile.newOutputStream.autoClosed} baseConf.writeXml(os)

    val zkPort = baseConf.get("hbase.zookeeper.property.clientPort")

    zkUtils = for {
      zkUtils <- makeZkUtils(zkPort)
    } yield zkUtils

    for (i <- 0 until NUMBROKERS)
      kafkaServers(i) = for {
        kafkaServer <- makeKafkaServer(s"localhost:$zkPort", i)
      } yield kafkaServer

    val brokerPort = kafkaServers.head.map[Int](server => {
      server.config.listeners.head._2.port
    }).getOrElse(throw new Exception("It shouldn't be here ..."))

    // setup producer
    val producerProps = new Properties()
    producerProps.setProperty("bootstrap.servers", s"$BROKERHOST:$brokerPort")
    producerProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    producer = new KafkaProducer[Array[Byte], Array[Byte]](producerProps)
    ()
  }

  override def afterAll(): Unit = {
    producer.close()
    shutdownKafkaServers()
    hbaseUtil.shutdownMiniCluster()
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
object ServiceSpec {

  private val (testDataPath, confPath) = {
    val testDataPath = s"${PathUtils.getTestDir(classOf[ServiceSpec]).getCanonicalPath}/MiniCluster"
    val confPath = s"$testDataPath/conf"
    (
      testDataPath.toFile.createIfNotExists(asDirectory = true, createParents = false),
      confPath.toFile.createIfNotExists(asDirectory = true, createParents = false)
    )
  }
}

class TestOpenTSDBConfigurator(mapConf: Map[String, String]) extends OpenTSDBConfigurator with Serializable {

  lazy val configuration: Configuration = mapConf.foldLeft(new Configuration(false)) { (conf, pair) =>
    conf.set(pair._1, pair._2)
    conf
  }

}

object TestOpenTSDBConfigurator {

  def apply(conf: Configuration): TestOpenTSDBConfigurator = new TestOpenTSDBConfigurator(
    conf.iterator().asScala.map { entry => entry.getKey -> entry.getValue }.toMap[String, String]
  )

}