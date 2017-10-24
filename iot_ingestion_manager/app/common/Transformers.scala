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

package common

import cats.FlatMap
import cats.data.Kleisli
import cats.implicits._
import common.Util._
import it.gov.daf.iotingestion.common.{EventType, SerializerDeserializer, StorableEvent}
import it.gov.daf.iotingestion.event.Event
import org.apache.spark.opentsdb.DataPoint
import play.Logger

import scala.language.{higherKinds, implicitConversions}
import scala.util.{Failure, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.ImplicitConversion",
    "org.wartremover.warts.ImplicitParameter",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Overloading",
    "org.wartremover.warts.DefaultArguments"
  )
)
object Transformers {

  @transient
  implicit private val alogger = Logger.of(this.getClass.getCanonicalName)

  trait transform[A, B] extends (A => Try[B]) with Serializable {
    override def apply(a: A): Try[B]
  }

  implicit class EnrichedKleisli[F[_], A, B](k: Kleisli[F, A, B]) extends AnyRef {
    def >>>>[C](f: B => F[C])(implicit Try: FlatMap[F]): Kleisli[F, A, C] = k.andThen(f)
  }

  implicit class EnrichedTransform[A, B](f1: A => Try[B]) {
    def >>>>[D, C](f2: B => Try[C]): Kleisli[Try, A, C] = Kleisli(f1) andThen f2
  }

  implicit def funcToKleisli[A, B](func: A => Try[B]): Kleisli[Try, A, B] = Kleisli(func)

  object avroByteArrayToEvent extends transform[Array[Byte], Event] {
    def apply(a: Array[Byte]): Try[Event] = SerializerDeserializer.deserialize(a)
  }

  object eventToStorableEvent extends transform[Event, StorableEvent] {
    def apply(a: Event): Try[StorableEvent] = Try {
      StorableEvent(
        version = a.version,
        id = a.id,
        ts = a.ts,
        temporal_granularity = a.temporal_granularity.getOrElse(null),
        event_certainty = a.event_certainty,
        event_type_id = a.event_type_id,
        event_subtype_id = a.event_subtype_id.getOrElse(null),
        event_annotation = a.event_annotation.getOrElse(null),
        source = a.source,
        location = a.location,
        body = a.body.getOrElse(null),
        attributesKeys = a.attributes.keys.mkString("#"),
        attributesValues = a.attributes.values.mkString("#")
      )
    }
  }

  object storableEventToDatapoint extends transform[StorableEvent, DataPoint[Double]] {

    def convertString(string: String): String = string.replaceAll("[^a-zA-Z0-9]+","")


    def apply(a: StorableEvent): Try[DataPoint[Double]] = {

      val eventType = EventType(a.event_type_id)
      eventType match {
        case EventType.Metric =>
          val attributes = (a.attributesKeys.split("#").zip(a.attributesValues.split("#"))).toMap
          val metricTry = for {
            metric <- attributes.get("metric").asTry(new RuntimeException("no metric name in attributes field"))
            valueString <- attributes.get("value").asTry(new RuntimeException("no metric value in attributes field"))
            value <- Try(valueString.toDouble)
          } yield (metric, value)

          metricTry.map {
            case (m, v) =>
            val tags = ("source", convertString(a.source)) :: attributes
              .getOrElse("tags", ",")
              .split(",").toList
              .flatMap { s =>
                val strim = s.trim
                attributes.get(strim).map((convertString(strim), _))
              }
            DataPoint[Double](m, a.ts, v, tags.toMap)
          }

        case _ => Failure(new RuntimeException("The event instance is not a Metric Event"))
      }

    }
  }

 /* object eventToDatapoint extends transform[Event, DataPoint[Double]] {
    def apply(a: Event): Try[DataPoint[Double]] = {

      val eventType = EventType(a.event_type_id)
      eventType match {
        case EventType.Metric =>

          val metricTry = for {
            metric <- a.attributes.get("metric").asTry(new RuntimeException("no metric name in attributes field"))
            valueString <- a.attributes.get("value").asTry(new RuntimeException("no metric value in attributes field"))
            value <- Try(valueString.toDouble)
          } yield (metric, value)

          metricTry.map { case (m, v) =>
            val tags = ("source", a.source) :: a.attributes
              .getOrElse("tags", ",")
              .split(",").toList
              .flatMap { s =>
                val strim = s.trim
                a.attributes.get(strim).map((convertString(strim), _))
              }
            DataPoint[Double](m, a.ts, v, tags.toMap)
          }

        case _ => Failure(new RuntimeException("The event instance is not a Metric Event"))
      }

    }
  }*/

}
