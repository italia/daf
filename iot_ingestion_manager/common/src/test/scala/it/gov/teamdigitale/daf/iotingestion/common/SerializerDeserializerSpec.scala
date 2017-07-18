package it.gov.teamdigitale.daf.iotingestion.common

import it.gov.daf.iotingestion.common.SerializerDeserializer
import it.gov.daf.iotingestion.event.Event
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
import org.specs2.mutable.Specification

import scala.util.Success

class SerializerDeserializerSpec extends Specification {
  "The SerDeser" should {
    "serialize and deserialize the events correctly" in {
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

      implicit val formats = DefaultFormats
      val event = parse(jsonEvent, true).extract[Event]
      val eventBytes = SerializerDeserializer.serialize(event)
      val Success(deserEvent) = SerializerDeserializer.deserialize(eventBytes)

      event must be equalTo (deserEvent)
    }
  }
}
