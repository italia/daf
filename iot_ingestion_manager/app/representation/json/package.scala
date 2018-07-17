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

import play.api.libs.json.{ Json, Reads }

package object json {

  implicit class JsonReadsSyntax[A](reads: Reads[A]) {

    def widen[B](implicit ev: A <:< B): Reads[B] = reads.map { ev }

  }

  val kafkaStreamDataReader = Json.reads[KafkaStreamData].widen[StreamData]

  val socketStreamDataReader = Json.reads[SocketStreamData].widen[StreamData]

  val streamDataReader = kafkaStreamDataReader orElse socketStreamDataReader

}
