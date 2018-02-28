package it.teamdigitale.managers

import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

import it.gov.daf.iotingestion.event.Event
import it.teamdigitale.miniclusters.{HDFSMiniCluster, KafkaLocalServer, KuduMiniCluster}
import it.teamdigitale.config.IotIngestionManagerConfig.{HdfsConfig, KafkaConfig, KuduConfig}
import it.teamdigitale.events.SerializerDeserializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.apache.kudu.spark.kudu._

import scala.util.{Failure, Success, Try}

class IotIngestionManagerSpec extends FlatSpec with Matchers with BeforeAndAfterAll{

  val kuducluster: KuduMiniCluster = new KuduMiniCluster()
  val hdfscluster: HDFSMiniCluster = new HDFSMiniCluster()
  val kafkaServer: KafkaLocalServer = new KafkaLocalServer()
  val kafkaConf: KafkaConfig = kafkaServer.getKafkaConf()
  var producer: KafkaProducer[Array[Byte], Array[Byte]] = _

  val sourceid = "http://domain/sensor/url"
  val day = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
  val sourceidencode = URLEncoder.encode(sourceid)

  override def beforeAll(): Unit = {

    kuducluster.start()
    hdfscluster.start()
    kafkaServer.start()

    kafkaServer.createTopic(kafkaConf.topic)
    val props = kafkaServer.getKafkaProps()
    producer = new KafkaProducer[Array[Byte], Array[Byte]](props)

    val metrics: Seq[Event] = Range(0,100).map(x => Event(
      version = 1L,
      id = x + "metric",
      ts = System.currentTimeMillis(),
      event_type_id = 0,
      location = "41.1260529:16.8692905",
      source = sourceid,
      body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
      attributes = Map(
        "tag1" -> "Via Cernaia(TO)",
        "tag2" -> "value2",
        "metric" -> "speed",
        "tags"-> "tag1, tag2")
    ))
    val genericEvents: Seq[Event] = Range(0,50).map(x =>  Event(
      version = 1L,
      id = x + "event",
      ts = System.currentTimeMillis(),
      event_type_id = 2,
      location = "41.1260529:16.8692905",
      source = sourceid,
      body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
      event_annotation = Some(s"This is a free text for $x"),
      attributes = Map(
        "tag1" -> "Via Cernaia(TO)",
        "tag2" -> "value2",
        "metric" -> "speed",
        "tags"-> "tag1, tag2")
    ))

    val events =  metrics ++ genericEvents
    sendOnKafka(events.toList)

  }

  def sendOnKafka(events: List[Event]) = {

    events
      .map(e => SerializerDeserializer.serialize(e))
      .map(bytes => new ProducerRecord[Array[Byte], Array[Byte]](kafkaConf.topic, bytes))
      .foreach{message =>
        producer.send(message)
        producer.flush()
      }

  }

  def readKudu(kuduConfig: KuduConfig): DataFrame = {
    kuducluster.sparkSession.sqlContext
      .read
      .options(Map("kudu.master" -> kuduConfig.masterAdresses,"kudu.table" -> kuduConfig.eventsTableName))
      .kudu
  }

  def readHdfs(dir: String): DataFrame = {
    kuducluster.sparkSession.read.parquet(dir)
  }

  "IotIngestion manager" should "correctly converts events" in {


    val metrics: Seq[Try[Event]] = Range(0,100).map(x => Success(Event(
      version = 1L,
      id = x + "metric",
      ts = System.currentTimeMillis(),
      event_type_id = 0,
      location = "41.1260529:16.8692905",
      source = "http://domain/sensor/url",
      body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
      attributes = Map(
        "tag1" -> "Via Cernaia(TO)",
        "tag2" -> "value2",
        "metric" -> "speed",
        "tags"-> "tag1, tag2")
    )))
    val genericEvents: Seq[Try[Event]] = Range(0,50).map(x =>  Success(Event(
      version = 1L,
      id = x + "event",
      ts = System.currentTimeMillis(),
      event_type_id = 2,
      location = "41.1260529:16.8692905",
      source = "http://domain/sensor/url",
      body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
      event_annotation = Some(s"This is a free text for $x"),
      attributes = Map(
        "tag1" -> "Via Cernaia(TO)",
        "tag2" -> "value2",
        "metric" -> "speed",
        "tags"-> "tag1, tag2")
    )))
    val failureEvents: Seq[Try[Event]]  = Seq (Failure(new RuntimeException("Hi, I'm an exception")))

    val events =  metrics ++ failureEvents ++ genericEvents

    val rdd = kuducluster.sparkSession.sparkContext.parallelize(events)
    val (metricsRDD, genericRDD) = IotIngestionManager.filterEvents(rdd)

    val metricsSize = metricsRDD.count
    val genericsSize = genericRDD.count

    metricsSize shouldBe 100
    genericsSize shouldBe 50
  }

  "IotIngestion manager" should "correctly store data in HDFS and Kudu" in {

    //val dataDirectory = System.getProperty("java.io.tmpdir")
    //val dir = new File(dataDirectory, "hdfsStorage")
    val dir = "./hdfsProva"

    val kuduConfig = KuduConfig(kuducluster.kuduMiniCluster.getMasterAddresses, "TestEvents", 2)
    val hdfsConfig = HdfsConfig(dir)

    IotIngestionManager.run(kuducluster.sparkSession, kuducluster.kuduContext, kafkaConf, kuduConfig, hdfsConfig)

    val metrics: Seq[Event] = Range(0,100).map(x => Event(
      version = 1L,
      id = x + "metric",
      ts = System.currentTimeMillis(),
      event_type_id = 0,
      location = "41.1260529:16.8692905",
      source = sourceid,
      body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
      attributes = Map(
        "tag1" -> "Via Cernaia(TO)",
        "tag2" -> "value2",
        "metric" -> "speed",
        "tags"-> "tag1, tag2")
    ))
    val genericEvents: Seq[Event] = Range(0,50).map(x =>  Event(
      version = 1L,
      id = x + "event",
      ts = System.currentTimeMillis(),
      event_type_id = 2,
      location = "41.1260529:16.8692905",
      source = sourceid,
      body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
      event_annotation = Some(s"This is a free text for $x"),
      attributes = Map(
        "tag1" -> "Via Cernaia(TO)",
        "tag2" -> "value2",
        "metric" -> "speed",
        "tags"-> "tag1, tag2")
    ))

    val events =  metrics ++ genericEvents
    sendOnKafka(events.toList)


    Thread.sleep(10000)
    producer.close()
    val dfKudu = readKudu(kuduConfig)
    //val dfHdfs = readHdfs(s"$dir/source=$sourceidencode/day=$day")

    dfKudu.count shouldBe 100
    //dfHdfs.count shouldBe 50

  }


  override def afterAll(): Unit = {
    kafkaServer.close()
    hdfscluster.close()
    kuducluster.close()
    IotIngestionManager.close()

  }

}
