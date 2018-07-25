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

import play.api.libs.json._
import representation.{ Sink, Source, StreamData }
import SourceReads.source
import SinkReads.sink

object StreamDataReads {

  private val fieldSchemaReads = for {
    fieldName <- (__ \ "name").read[String]
    fieldType <- (__ \ "type").read[String]
  } yield (fieldName, fieldType)

  private val schemaReads = (__ \ "schema").read[Seq[(String, String)]](Reads.seq(fieldSchemaReads))

  val streamData = for {
    id       <- (__ \ "id").read[String]
    interval <- (__ \ "interval").read[Long]
    owner    <- (__ \ "owner").read[String]
    source   <- (__ \ "source").read[Source]
    sink     <- (__ \ "sink").read[Sink]
    schema   <- schemaReads
  } yield StreamData(
    id       = id,
    interval = interval,
    owner    = owner,
    source   = source,
    sink     = sink,
    schema   = schema.toMap[String, String]
  )

}
