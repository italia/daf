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

package representation

import io.swagger.annotations.ApiModel

@ApiModel("Event")
final case class Event(id: String,
                 source: String,
                 version: Option[Long],
                 timestamp: Long,
                 location: Option[EventLocation],
                 certainty: Option[Double],
                 eventType: EventType,
                 customType: Option[String],
                 comment: Option[String],
                 body: String,
                 attributes: Map[String, Any])

sealed trait EventType

case object MetricEventType extends EventType
case object StateChangedEventType extends EventType
case object OtherEventType extends EventType

final case class EventLocation(latitude: Double, longitude: Double)

object EventTypes {

  def fromString(s: String) = s.toLowerCase match {
    case "metric"       => MetricEventType
    case "state-change" => StateChangedEventType
    case _              => OtherEventType
  }

}
