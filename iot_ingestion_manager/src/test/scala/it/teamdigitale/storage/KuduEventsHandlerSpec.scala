package it.teamdigitale.storage

import java.io.File
import java.util.concurrent.TimeUnit

import org.apache.kudu.spark.kudu._
import it.teamdigitale.miniclusters.KuduMiniCluster
import it.teamdigitale.config.IotIngestionManagerConfig.KuduConfig
import it.teamdigitale.managers.IotIngestionManager
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import it.gov.daf.iotingestion.event.Event
import it.teamdigitale.EventModel.{EventToKuduEvent, EventToStorableEvent}
import org.apache.logging.log4j.LogManager

import scala.util.{Failure, Success, Try}

class KuduEventsHandlerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val logger = LogManager.getLogger(this.getClass)
  val kuduCluster = new KuduMiniCluster()

  val metrics: Seq[Try[Event]] = Range(0,100).map(x => Success( Event(
    version = 1L,
    id = x + "metric",
    ts = System.currentTimeMillis() + x ,
    event_type_id = 0,
    location = "41.1260529:16.8692905",
    source = "http://domain/sensor/url",
    body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
    event_subtype_id = Some("Via Cernaia(TO)"),
    attributes = Map("value" -> x.toString)
  )))

  val rdd = kuduCluster.sparkSession.sparkContext.parallelize(metrics)


  "KuduEventsHandler" should "store correctly data" in {

   val metricsRDD = rdd
      .map(event => EventToStorableEvent(event))
      .flatMap(e => e.toOption)
      .map(se => EventToKuduEvent(se)).flatMap(e => e.toOption)

    val metricsDF = kuduCluster.sparkSession.createDataFrame(metricsRDD)

    val kuduConfig = KuduConfig(kuduCluster.kuduMiniCluster.getMasterAddresses, "TestEvents", 2)

    KuduEventsHandler.getOrCreateTable(kuduCluster.kuduContext, kuduConfig)
    KuduEventsHandler.write(metricsDF, kuduCluster.kuduContext, kuduConfig)

    val df = kuduCluster.sparkSession.sqlContext
      .read
      .options(Map("kudu.master" -> kuduConfig.masterAdresses,"kudu.table" -> kuduConfig.eventsTableName))
      .kudu

    df.count shouldBe 100

  }

  "KuduEventsHandler" should "handle redundant data" in {

    val metricsRDD = rdd
      .map(event => EventToStorableEvent(event))
      .flatMap(e => e.toOption)
      .map(se => EventToKuduEvent(se))
      .flatMap(e => e.toOption)

    val metricsDF = kuduCluster.sparkSession.createDataFrame(metricsRDD)

    val kuduConfig = KuduConfig(kuduCluster.kuduMiniCluster.getMasterAddresses, "TestEventsDuplicate", 2)
    KuduEventsHandler.getOrCreateTable(kuduCluster.kuduContext, kuduConfig)

    KuduEventsHandler.write(metricsDF, kuduCluster.kuduContext, kuduConfig)
    KuduEventsHandler.write(metricsDF, kuduCluster.kuduContext, kuduConfig)

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
