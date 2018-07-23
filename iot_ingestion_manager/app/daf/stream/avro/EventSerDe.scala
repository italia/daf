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

package daf.stream.avro

import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs
import it.gov.daf.iot.event.Event

import scala.util.Try

object EventSerDe {

  private val specificAvroBinaryInjection: Injection[Event, Array[Byte]] = SpecificAvroCodecs.toBinary[Event]

  def deserialize(bytes: Array[Byte]): Try[Event] = specificAvroBinaryInjection.invert(bytes)

  def serialize(event: Event): Array[Byte] = specificAvroBinaryInjection(event)

}
