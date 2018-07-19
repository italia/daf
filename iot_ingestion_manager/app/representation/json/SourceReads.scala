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

import play.api.data.validation.ValidationError
import play.api.libs.json._
import representation.{ KafkaSource, SocketSource, Source }

object SourceReads {

  private def invalidSourceType(value: String) = ValidationError(s"Invalid source type [$value]: must be one of [kafka | socket]")

  private val kafkaSource = (__ \ "topic").read[String].map { KafkaSource }

  private val socketSource = for {
    host <- (__ \ "host").read[String]
    port <- (__ \ "port").read[Int]
  } yield SocketSource(host, port)


  implicit val source: Reads[Source] = (__ \ "type").read[String].flatMap {
    case "kafka"  => kafkaSource.widen[Source]
    case "socket" => socketSource.widen[Source]
    case other    => readError { invalidSourceType(other) }
  }

}
