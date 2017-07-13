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
import it.gov.teamdigitale.iotingestion.common.SerializerDeserializer
import org.apache.spark.opentsdb.DataPoint
import it.gov.teamdigitale.iotingestion.event.Event

import scala.language.{higherKinds, implicitConversions}
import scala.util.{Random, Try}

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
    override def apply(a: Event): Try[DataPoint[Double]] = Try {
      new DataPoint[Double]("metric", 12345678L, 0.1D, Map.empty[String, String])
    }
  }
  
}
