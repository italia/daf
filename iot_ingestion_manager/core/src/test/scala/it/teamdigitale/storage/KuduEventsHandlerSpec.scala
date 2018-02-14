package it.teamdigitale.storage

import java.io.File
import java.util.concurrent.TimeUnit
import org.apache.kudu.spark.kudu._
import it.teamdigitale.baseSpec.KuduMiniCluster
import it.teamdigitale.config.IotIngestionManagerConfig.KuduConfig
import it.teamdigitale.managers.IotIngestionManager
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import it.gov.daf.iotingestion.event.Event
import scala.util.{Failure, Success, Try}

class KuduEventsHandlerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val logger = LoggerFactory.getLogger(this.getClass)
  val kuduCluster = new KuduMiniCluster()

  val sourceid = "http://domain/sensor/url"
  val day = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

  val dataDirectory = System.getProperty("java.io.tmpdir")
  val dir = new File(dataDirectory, "hdfsStorage")
  //val path = s"$dataDirectory/hdfs/$testName"

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

  val failureEvents: Seq[Try[Event]]  = Seq (Failure(new RuntimeException("Hi, I'm an exception")))

  val events =  metrics ++ failureEvents ++ genericEvents

  val rdd = kuduCluster.sparkSession.sparkContext.parallelize(events)


  "KuduEventsHandler" should "store correctly data" in {

    val(metricsRDD, genericRDD) = IotIngestionManager.filterEvents(rdd)
    val metricsDF = kuduCluster.sparkSession.createDataFrame(metricsRDD)

    val kuduConfig = KuduConfig(kuduCluster.kuduMiniCluster.getMasterAddresses, "TestEvents", 2)
    KuduEventsHandler.getOrCreateTable(kuduCluster.kuduContext, kuduConfig)

    kuduCluster.kuduContext.insertRows(metricsDF, kuduConfig.eventsTableName)

    val df = kuduCluster.sparkSession.sqlContext
      .read
      .options(Map("kudu.master" -> kuduConfig.masterAdresses,"kudu.table" -> kuduConfig.eventsTableName))
      .kudu

    df.count shouldBe 100

  }

  "KuduEventsHandler" should "handle redundant data" in {

    val(metricsRDD, genericRDD) = IotIngestionManager.filterEvents(rdd)
    val metricsDF = kuduCluster.sparkSession.createDataFrame(metricsRDD)

    val kuduConfig = KuduConfig(kuduCluster.kuduMiniCluster.getMasterAddresses, "TestEventsDuplicate", 2)
    KuduEventsHandler.getOrCreateTable(kuduCluster.kuduContext, kuduConfig)

    kuduCluster.kuduContext.insertIgnoreRows(metricsDF, kuduConfig.eventsTableName)
    kuduCluster.kuduContext.insertIgnoreRows(metricsDF, kuduConfig.eventsTableName)

    val df = kuduCluster.sparkSession.sqlContext
      .read
      .options(Map("kudu.master" -> kuduConfig.masterAdresses,"kudu.table" -> kuduConfig.eventsTableName))
      .kudu

    df.count shouldBe 100

  }

  override def beforeAll() {
    kuduCluster.start()
  }

  override def afterAll() {
    kuduCluster.start()
  }

}
