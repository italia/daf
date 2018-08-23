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

import org.scalatest.{ MustMatchers, WordSpec }
import representation.StreamData
import daf.stream.AvroJsonPayloadValidator
import play.api.libs.json._

class AvroJsonPayloadValidatorSpec extends WordSpec with MustMatchers {

  "Avro Payload Validation" must {

    "pass" when {

      "given valid data" in {
        AvroJsonPayloadValidator.validate(AvroJsonPayloads.conforming, AvroJsonPayloads.streamData) must be { 'Success }
      }

      "given data with missing fields" in {
        AvroJsonPayloadValidator.validate(AvroJsonPayloads.missingFields, AvroJsonPayloads.streamData) must be { 'Success }
      }
    }

    "fail" when {

      "given data with wrong types" in {
        AvroJsonPayloadValidator.validate(AvroJsonPayloads.nonConforming, AvroJsonPayloads.streamData) must be { 'Failure }
      }

      "given data with unknown fields" in {
        AvroJsonPayloadValidator.validate(AvroJsonPayloads.extraField, AvroJsonPayloads.streamData) must be { 'Failure }
      }

    }

  }

}

private object AvroJsonPayloads {

  val schema = Map(
    "int-attr"     -> "int",
    "double-attr"  -> "double",
    "string-attr"  -> "string",
    "long-attr"    -> "long",
    "boolean-attr" -> "boolean",
    "object-attr"  -> "record",
    "array-attr"   -> "array"
  )

  val streamData = StreamData(
    id       = "",
    interval = 10,
    owner    = "",
    source   = null,
    sink     = null,
    schema   = schema
  )

  val conforming = Map(
    "int-attr"     -> JsNumber(1),
    "double-attr"  -> JsNumber(0.2d),
    "string-attr"  -> JsString("some string"),
    "long-attr"    -> JsNumber(10l),
    "boolean-attr" -> JsBoolean(false),
    "object-attr"  -> JsObject { Seq.empty },
    "array-attr"   -> JsArray { Seq.empty }
  )

  val nonConforming = Map(
    "int-attr"     -> JsString("1"),
    "double-attr"  -> JsBoolean(false),
    "string-attr"  -> JsString("string"),
    "long-attr"    -> JsNumber(1l),
    "boolean-attr" -> JsBoolean(true)
  )

  val extraField = conforming + { "extra-int-field" -> 1 }

  val missingFields = conforming - "boolean-attr"

}