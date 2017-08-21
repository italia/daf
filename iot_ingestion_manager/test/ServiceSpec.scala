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
import better.files.{File, _}
import it.gov.daf.iotingestion.common.SerializerDeserializer
import it.gov.daf.iotingestion.event.Event
import it.gov.daf.iotingestionmanager.client.Iot_ingestion_managerClient
import kafka.admin.{AdminUtils, RackAwareMode}
import kafka.server.{KafkaConfig, KafkaServer, RunningAsBroker}
import kafka.utils.{MockTime, TestUtils, ZkUtils}
import net.opentsdb.core._
import net.opentsdb.utils.Config
import org.I0Itec.zkclient.ZkClient
import org.I0Itec.zkclient.serialize.ZkSerializer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseTestingUtility, TableName}
import org.apache.hadoop.test.PathUtils
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.opentsdb.OpenTSDBConfigurator
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.Logger
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test.WithServer
import shaded.org.hbase.async.HBaseClient

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

  private val TIMEOUT = 10000

  private val NUMBROKERS = 1

  private val BROKERHOST = "127.0.0.1"

  private var zkUtils: Try[ZkUtils] = Failure[ZkUtils](new Exception(""))

  private var kafkaServers: Array[Try[KafkaServer]] = new Array[Try[KafkaServer]](NUMBROKERS)

  private var logDir: Try[JFile] = Failure[JFile](new Exception(""))

  private var producer: KafkaProducer[Array[Byte], Array[Byte]] = _

  var tsdb: TSDB = _

  var hbaseAsyncClient: HBaseClient = _

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
    configure("kafka.zookeeper.quorum" -> s"localhost:${baseConf.get("hbase.zookeeper.property.clientPort")}").
    build()

  "The iot-ingestion-manager" should {
    "receive and store the metric timeseries in OpenTSDB correctly" in new WithServer(app = application, port = getAvailablePort) {
      val ws: AhcWSClient = AhcWSClient()

      val plainCreds = "david:david"
      val plainCredsBytes = plainCreds.getBytes
      val base64CredsBytes = Base64.getEncoder.encode(plainCredsBytes)
      val base64Creds = new String(base64CredsBytes)
      val client = new Iot_ingestion_managerClient(ws)(s"http://localhost:$port")
      val result1 = Await.result(client.startOpenTSDB(s"Basic $base64Creds"), Duration.Inf)

      Thread.sleep(10000)

      var start = System.currentTimeMillis()
      val NUMEVENTS = 200
      val events = Range(0, NUMEVENTS).map(r =>
        new Event(
          version = 1L,
          id = Some(r.toString),
          ts = start + r,
          event_type_id = 0,
          location = "41.1260529:16.8692905",
          source = "sensor_id",
          body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
          attributes = Map(
            "tag1" -> "value1",
            "tag2" -> "value2",
            "metric" -> "speed",
            "value" -> "50",
            "tags" -> "tag1, tag2"
          )
        )
      )

      events.map { event =>
        val eventBytes = SerializerDeserializer.serialize(event)

        val record = new ProducerRecord[Array[Byte], Array[Byte]]("daf-iot-events", s"${event.id.getOrElse("error")}".getBytes(), eventBytes)
        producer.send(record)
        producer.flush()
      }

      Thread.sleep(5000)

      val result2 = Await.result(client.stopOpenTSDB(s"Basic $base64Creds"), Duration.Inf)

      val query = new TSQuery()
      query.setStart("10h-ago")

      val subQuery = new TSSubQuery()
      subQuery.setMetric("speed")
      subQuery.setAggregator("sum")

      val subQueries = new java.util.ArrayList[TSSubQuery]()
      subQueries.add(subQuery)
      query.setQueries(subQueries)
      query.setMsResolution(true)
      query.validateAndSetQuery()
      val tsdbQueries: Array[Query] = query.buildQueries(tsdb)
      val res = tsdbQueries.head.runAsync()
      val dp: Array[DataPoints] = res.join(1000L)
      dp.map(dp => dp.iterator().asScala.size) must beEqualTo(Array(NUMEVENTS))
    }
  }

  private val hbaseUtil = new HBaseTestingUtility()

  private var baseConf: Configuration = _

  override def beforeAll(): Unit = {
    hbaseUtil.startMiniCluster(4)
    Logger.info("Started the HBase minicluster")

    val conf = new SparkConf().
      setAppName("daf-iot-manager-local-test").
      setMaster("local[4]").
      set("spark.io.compression.codec", "lzf")
    baseConf = hbaseUtil.getConfiguration
    hbaseUtil.createTable(TableName.valueOf("tsdb-uid"), Array("id", "name"))
    hbaseUtil.createTable(TableName.valueOf("tsdb"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-tree"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-meta"), Array("name"))
    Logger.info("Created the HBase OpenTSDB tables")

    val confFile: File = confPath / "hbase-site.xml"
    for {os <- confFile.newOutputStream.autoClosed} baseConf.writeXml(os)

    val zkPort = baseConf.get("hbase.zookeeper.property.clientPort")

    zkUtils = for {
      zkUtils <- makeZkUtils(zkPort)
    } yield zkUtils
    zkUtils.foreach(_ => Logger.info("Created the zkUtils"))
    if (zkUtils.isFailure)
      Logger.info(s"Problem: Cannot create the zkUtils: ${zkUtils.failed.map(_.getMessage)}")

    for (i <- 0 until NUMBROKERS) {
      kafkaServers(i) = {
        val server = for {
          kafkaServer <- makeKafkaServer(s"localhost:$zkPort", i)
        } yield kafkaServer
        server.foreach(_ => Logger.info("Created the Kafka brokers"))
        if (server.isFailure)
          Logger.info(s"Problem: Cannot create the kafka broker: ${server.failed.map(_.getMessage)}")
        server
      }
    }

    val brokerPort = kafkaServers.head.map[Int](server => {
      server.config.listeners.head._2.port
    }).getOrElse(throw new Exception("It shouldn't be here ..."))

    zkUtils.foreach(zku => {
      AdminUtils.createTopic(zku, "daf-iot-events", 1, 1, new Properties(), RackAwareMode.Disabled)

      while (zku.getPartitionsForTopics(Seq("daf-iot-events")).isEmpty) {
        Logger.info("Waiting for the topic to be created ...")
        Thread.sleep(1000)
      }
      Logger.info("Created the topic")
    })

    // setup producer
    val producerProps = new Properties()
    producerProps.setProperty("bootstrap.servers", s"$BROKERHOST:$brokerPort")
    producerProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    producer = new KafkaProducer[Array[Byte], Array[Byte]](producerProps)
    Logger.info("Created the Kafka producer")

    hbaseAsyncClient = new HBaseClient(s"localhost:$zkPort", "/hbase")
    val config = new Config(false)
    config.overrideConfig("tsd.storage.hbase.data_table", "tsdb")
    config.overrideConfig("tsd.storage.hbase.uid_table", "tsdb-uid")
    config.overrideConfig("tsd.core.auto_create_metrics", "true")
    config.overrideConfig("batchSize", "10")
    config.disableCompactions()
    tsdb = new TSDB(hbaseAsyncClient, config)
    Logger.info("Created the hbase client and the opentsdb object")

    Thread.sleep(10000)
    ()
  }

  override def afterAll(): Unit = {
    producer.close()
    shutdownKafkaServers()
    tsdb.shutdown()
    hbaseAsyncClient.shutdown()
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