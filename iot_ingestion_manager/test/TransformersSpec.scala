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

import common.Transformers._
import it.gov.daf.iotingestion.event.Event
import org.specs2.mutable.Specification

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.TryPartial"
  )
)
class TransformersSpec extends Specification {

  "The Transformers Test" should {
    "Fail when the metric field is not present" in {
      val event = new Event(
        version = 1L,
        id = "example without metric",
        ts = System.currentTimeMillis(),
        event_type_id = 0,
        location = "41.1260529:16.8692905",
        source = "http://domain/sensor/url",
        body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
        attributes = Map(
          "tag1" -> "Via Cernaia(TO)",
          "tag2" -> "value2",
          "value" -> "50",
          "tags"-> "tag1, tag2"
        )
      )
      val res = (eventToStorableEvent >>>> storableEventToDatapoint)(event)
      res must beFailedTry

    }
      "Fail when the value field is not present" in {
        val event = new Event(
          version = 1L,
          id = "example without metric",
          ts = System.currentTimeMillis(),
          event_type_id = 0,
          location = "41.1260529:16.8692905",
          source = "http://domain/sensor/url",
          body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
          attributes = Map(
            "tag1" -> "Via Cernaia(TO)",
            "tag2" -> "value2",
            "metric" -> "speed",
            "tags"-> "tag1, tag2"
          )
        )
        val res = (eventToStorableEvent >>>> storableEventToDatapoint)(event)
        println(res)
        res must beFailedTry

      }

      "Ignore tags that are not present in the attributes field" in {
        val event = new Event(
          version = 1L,
          id = "example without metric",
          ts = System.currentTimeMillis(),
          event_type_id = 0,
          location = "41.1260529:16.8692905",
          source = "http://domain/sensor/url",
          body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
          attributes = Map(
            "tag1" -> "Via Cernaia(TO)",
            "tag2" -> "value2",
            "value" -> "50",
            "metric" -> "speed",
            "tags"-> "tag1, tag2, tag3"
          )
        )
        val res = (eventToStorableEvent >>>> storableEventToDatapoint)(event)
        println(res)
        res must beSuccessfulTry

        res.get.tags.contains("tag3") mustEqual false
        res.get.tags.contains("tag2") mustEqual true
        res.get.tags.contains("tag1") mustEqual true

      }

    "convert events into datapoints" in {

      val events = Range(0, 100).map(r =>
        new Event(
          version = 1L,
          id = r.toString,
          ts = System.currentTimeMillis(),
          event_type_id = 0,
          location = "41.1260529:16.8692905",
          source = "http://domain/sensor/url",
          body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
          attributes = Map(
            "tag1" -> "value1",
            "tag2" -> "value2",
            "metric" -> "speed",
            "value" -> "50",
            "tags"-> "tag1, tag2"
          )
        )
      )
      println(events.head)

      val dataPoints = events.map(e => (eventToStorableEvent >>>> storableEventToDatapoint)(e))
      //dataPoints.foreach(d => println(d))

      dataPoints.count(_.isSuccess) mustEqual 100
      val head = dataPoints.head.get
      head.tags.size mustEqual 3
      head.metric mustEqual "speed"
    }
  }

}
