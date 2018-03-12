package it.teamdigitale.events

import it.gov.daf.iotingestion.event.Event
import it.teamdigitale.EventModel.EventToKuduEvent
import it.teamdigitale.EventModel.EventToStorableEvent
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Success, Try}

class EventToKuduEventSpec extends FlatSpec with Matchers{

  val metrics = Range(0,100).map(x => Success( Event(
    version = 1L,
    id = x + "metric",
    ts = System.currentTimeMillis(),
    event_type_id = 0,
    location = "41.1260529:16.8692905",
    source = "http://domain/sensor/url",
    body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
    event_subtype_id = Some("SPEED_Via_Cernaia_TO"),
    attributes = Map(
      "value" -> x.toString)
  )))

  // this metric doesn't have any value
  val wrongMetric = Success( Event(
    version = 1L,
    id = "wrongmetric1",
    ts = System.currentTimeMillis(),
    event_type_id = 0,
    location = "41.1260529:16.8692905",
    source = "http://domain/sensor/url",
    body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
    event_annotation = Some(s"This is a free text for a wrong metric"),
    event_subtype_id = Some("SPEED_Via_Cernaia_TO"),
    attributes = Map()
  ))

  // this metric doesn't have a correct value
  val wrongMetric2 = Success( Event(
    version = 1L,
    id = "wrongmetric2",
    ts = System.currentTimeMillis(),
    event_type_id = 0,
    location = "41.1260529:16.8692905",
    source = "http://domain/sensor/url",
    body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
    event_annotation = Some(s"This is a free text ©"),
    event_subtype_id = Some("SPEED_Via_Cernaia_TO"),
    attributes = Map(
      "value" -> "wrongValue"
    )
  ))

  // this metric doesn't have the metric id
  val wrongMetric3 = Success( Event(
    version = 1L,
    id = "wrongmetric3",
    ts = System.currentTimeMillis(),
    event_type_id = 2,
    location = "41.1260529:16.8692905",
    source = "http://domain/sensor/url",
    body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
    event_annotation = Some(s"This is a free text for a wrong metric"),
    attributes = Map(
      "value" -> "100"
    )
  ))


  "Correct events" should "be converted" in {
    val res = metrics.map(event => EventToStorableEvent(event)).flatMap(_.toOption).map(event => EventToKuduEvent(event)).filter(_.isSuccess)
    res.length shouldBe 100
    res.head.get.metric shouldBe 0D
    res.head.get.metric_id shouldBe "SPEED_Via_Cernaia_TO"
  }

  "Wrong events" should "be filtered" in {
    val seq = metrics ++ List(wrongMetric, wrongMetric2, wrongMetric3)

    val res = seq.map(event => EventToStorableEvent(event)).flatMap(_.toOption).map(event => EventToKuduEvent(event)).filter(_.isSuccess)
    res.length shouldBe 100
  }
}
