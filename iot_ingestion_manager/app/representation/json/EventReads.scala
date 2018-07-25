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

package representation.json

import cats.Id
import cats.free.Free
import play.api.libs.json._
import representation.{ Event, EventLocation, EventTypes }

object EventReads {

  private type Trampoline[A] = Free[Id, A]
  private type JsAttributes = Seq[(String, JsValue)]

  private val eventTypeReads = (__ \ "eventType").read[String].map { EventTypes.fromString }

  private def __flatten(key: String, jsAttributes: JsAttributes, tail: JsAttributes, acc: JsAttributes): Trampoline[JsAttributes] =
    _prefix(key, jsAttributes, tail).flatMap { _flatten(_, acc) }

  private def _prefix(prefix: String, jsAttributes: JsAttributes, tail: JsAttributes): Trampoline[JsAttributes] = Free.pure[Id, JsAttributes] {
    tail ++ jsAttributes.map { case (k, v) => s"$prefix.$k" -> v }
  }

  private def _flatten(jsAttributes: JsAttributes, acc: JsAttributes = Seq.empty): Trampoline[JsAttributes] = jsAttributes match {
    case Seq((key, jsObject: JsObject), tail @ _*) => Free.defer { __flatten(key, jsObject.value.toList, tail, acc) }
    case Seq(head, tail @ _*)                      => Free.defer { _flatten(tail, acc :+ head) }
    case _                                         => Free.pure[Id, JsAttributes] { acc }
  }

  private def unmarshall(jsAttributes: JsAttributes): Map[String, Any] = jsAttributes.map {
    case (key, JsNumber(value)) if value.isValidInt  => key -> value.toIntExact
    case (key, JsNumber(value)) if value.isValidLong => key -> value.toLongExact
    case (key, JsNumber(value))                      => key -> value.toDouble
    case (key, JsBoolean(value))                     => key -> value
    case (key, JsString(value))                      => key -> value
    case (key, jsValue)                              => key -> Json.stringify(jsValue)
  }.toMap[String, Any]

  private val attributeReads = (__ \ "attributes").readNullable[JsObject].map {
    _.map { jsObject =>
      _flatten(jsObject.value.toList).map { unmarshall }.run
    }
  }

  private val payloadReads = (__ \ "payload").read[JsObject].map { jsObject =>
    _flatten(jsObject.value.toList).map { unmarshall }.run
  }

  private val locationReads = (__ \ "location").readNullable[EventLocation](Json.reads[EventLocation])

  implicit val event: Reads[Event] = for {
    id         <- (__ \ "id").read[String]
    source     <- (__ \ "source").read[String]
    version    <- (__ \ "version").readNullable[Long]
    timestamp  <- (__ \ "timestamp").read[Long]
    location   <- locationReads
    certainty  <- (__ \ "certainty").readNullable[Double]
    eventType  <- eventTypeReads
    customType <- (__ \ "customType").readNullable[String]
    comment    <- (__ \ "comment").readNullable[String]
    payload    <- payloadReads
    attributes <- attributeReads
  } yield Event(
    id         = id,
    source     = source,
    version    = version,
    timestamp  = timestamp,
    location   = location,
    certainty  = certainty,
    eventType  = eventType,
    customType = customType,
    comment    = comment,
    payload    = payload,
    attributes = attributes.getOrElse { Map.empty[String, Any] }
  )

}
