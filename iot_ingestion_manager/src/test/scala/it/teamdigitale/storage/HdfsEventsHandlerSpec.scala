package it.teamdigitale.storage

import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import it.teamdigitale.miniclusters.HDFSMiniCluster
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import it.gov.daf.iotingestion.event.Event
import it.teamdigitale.EventModel.{EventToHdfsEvent, EventToStorableEvent}
import it.teamdigitale.config.IotIngestionManagerConfig.HdfsConfig
import it.teamdigitale.events.SerializerDeserializer
import it.teamdigitale.managers.IotIngestionManager
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

class HdfsEventsHandlerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val logger = LoggerFactory.getLogger(this.getClass)

  var hdfsCluster: HDFSMiniCluster= new HDFSMiniCluster()
  val testName = s"test-${System.currentTimeMillis()}"
  var sparkSession = SparkSession.builder().appName(testName).master("local").getOrCreate()


  val sourceid = "http://domain/sensor/url"
  val day = new SimpleDateFormat("ddMMyyyy").format(new Date(System.currentTimeMillis()))

  val dataDirectory = System.getProperty("java.io.tmpdir")
  val dir = new File(dataDirectory, "hdfsStorage")

  val metrics: Seq[Try[Event]] = Range(0,100).map(x => Success( Event(
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

  val genericEvents: Seq[Try[Event]] = Range(0,50).map(x => Success( Event(
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
  )))

  val wrongEvents: Seq[Try[Event]] = Range(0,5).map(x => Success( Event(
    version = 1L,
    id = x + "event",
    ts = -1,
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
  )))

  val failureEvents: Seq[Try[Event]]  = Seq (Failure(new RuntimeException("Hi, I'm an exception")))

  val events =  metrics ++ failureEvents ++ genericEvents ++ wrongEvents

  val rdd = sparkSession.sparkContext.parallelize(events)


  override def beforeAll() {
    hdfsCluster.start()
  }

  override  def afterAll(): Unit = {
    hdfsCluster.close()

    if (dir.exists())
      FileUtils.deleteDirectory(dir)
  }

  "HdfsEventsHandler" should "partitioning data based on source and day" in {

    val hdfsEventsHandler = HdfsEventsHandler.partitionList
    List("source", "day") shouldBe HdfsEventsHandler.partitionList

    val hdfsRDD = rdd
      .map(event => EventToStorableEvent(event))
      .flatMap(e => e.toOption)
      .map(se => EventToHdfsEvent(se))
      .flatMap(_.toOption)

    val genericEventsDF = sparkSession.createDataFrame(hdfsRDD)

    val hdfsConfig = HdfsConfig(dir.getAbsolutePath)
    HdfsEventsHandler.write(genericEventsDF, hdfsConfig)
    val sourceidencode = URLEncoder.encode(sourceid)

    logger.info(s"HDFS storage path: ${dir.getAbsolutePath}")

    val df = sparkSession.read.parquet(s"$dir/source=$sourceidencode/day=$day")
    df.count shouldBe 150

  }

}
