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
import it.gov.teamdigitale.daf.iotingestion.common.SerializerDeserializer
import it.gov.teamdigitale.daf.iotingestion.common.EventType
import it.gov.teamdigitale.daf.iotingestion.event.Event
import org.apache.spark.opentsdb.DataPoint

import scala.language.{higherKinds, implicitConversions}
import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.ImplicitConversion",
    "org.wartremover.warts.ImplicitParameter",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Null"
  )
)
object Transformers {

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

  object eventToDatapoint extends transform[Event, DataPoint[Double]] {
    override def apply(a: Event): Try[DataPoint[Double]] = {

      val eventType = EventType(a.event_type_id)
      eventType match {
        case EventType.Metric =>
          a.attributes.get("metric")


//          val tagsMap = a.attributes ++
//            Map("source" -> a.source,
//              "event_certainty" -> a.event_certainty)
//          Success(new DataPoint[Double](a.event_type_id, a.ts, m, a.attributes))
          Failure(new RuntimeException("no metric value"))
        case _ => Failure(new RuntimeException("no metric value"))
      }

    }
  }

}
