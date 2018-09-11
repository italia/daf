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

import daf.stream.AvroPayloadValidator
import org.scalatest.{ MustMatchers, WordSpec }
import representation.StreamData

class AvroPayloadValidatorSpec extends WordSpec with MustMatchers {

  "Avro Payload Validation" must {

    "pass" when {

      "given valid data" in {
        AvroPayloadValidator.validate(AvroPayloads.conforming, AvroPayloads.streamData) must be { 'Success }
      }

      "given data with missing fields" in {
        AvroPayloadValidator.validate(AvroPayloads.missingFields, AvroPayloads.streamData) must be { 'Success }
      }
    }

    "fail" when {

      "given data with wrong types" in {
        AvroPayloadValidator.validate(AvroPayloads.nonConforming, AvroPayloads.streamData) must be { 'Failure }
      }

      "given data with unknown fields" in {
        AvroPayloadValidator.validate(AvroPayloads.extraField, AvroPayloads.streamData) must be { 'Failure }
      }

    }

  }

}

private object AvroPayloads {

  val schema = Map(
    "int-attr"     -> "int",
    "double-attr"  -> "double",
    "string-attr"  -> "string",
    "long-attr"    -> "long",
    "boolean-attr" -> "boolean"
  )

  val streamData = StreamData(
    id          = "",
    name        = "",
    owner       = "",
    group       = "",
    description = "",
    domain      = "",
    subDomain   = "",
    isOpenData  = true,
    isStandard  = true,
    interval    = 10,
    source      = null,
    sink        = null,
    schema      = schema
  )

  val conforming = Map(
    "int-attr"     -> 1,
    "double-attr"  -> 0.2d,
    "string-attr"  -> "some string",
    "long-attr"    -> 10l,
    "boolean-attr" -> false
  )

  val nonConforming = Map(
    "int-attr"     -> "1",
    "double-attr"  -> false,
    "string-attr"  -> "string",
    "long-attr"    -> 1l,
    "boolean-attr" -> true
  )

  val extraField = conforming + { "extra-int-field" -> 1 }

  val missingFields = conforming - "boolean-attr"


}