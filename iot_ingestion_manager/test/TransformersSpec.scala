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

import common.Transformers
import it.gov.teamdigitale.daf.iotingestion.event.Event
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
    "convert events into datapoints" in {

      val events = Range(0, 100).map(r =>
        new Event(
          version = 0L,
          id = Option(r.toString), //here we should put something as "traffic"
          ts = System.currentTimeMillis(),
          event_type_id = "traffic".hashCode,
          location = "41.1260529:16.8692905",
          host = "http://domain/sensor",
          service = "http://domain/sensor/url",
          body = Option("""{"rowdata": "this json should contain row data"}""".getBytes()),
          attributes = Map(
            "tag1" -> "value1",
            "tag2" -> "value2",
            "metric" -> "10"
          )
        )
      )

      val dataPoints = events.map(e => Transformers.eventToDatapoint(e))
      dataPoints.count(_.isSuccess) mustEqual 100
      val head = dataPoints.head.get
      head.tags.size mustEqual 5
      head.metric mustEqual "traffic".hashCode.toString
    }
  }

}

