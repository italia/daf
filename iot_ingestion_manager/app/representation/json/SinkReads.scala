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
import representation.{ ConsoleSink, HdfsSink, KuduSink, Sink }

object SinkReads {

  private def invalidSinkType(value: String) = ValidationError(s"Invalid sink type [$value]: must be one of [hdfs | kudu | console]")

  private val hdfsSink = (__ \ "path").read[String].map { HdfsSink }

  private val kuduSink = (__ \ "table").read[String].map { KuduSink }

  implicit val sink: Reads[Sink] = (__ \ "type").read[String].flatMap {
    case "hdfs"    => hdfsSink.widen[Sink]
    case "kudu"    => kuduSink.widen[Sink]
    case "console" => Reads[Sink] { _ => JsSuccess(ConsoleSink) }
    case other     => readError { invalidSinkType(other) }
  }

}
