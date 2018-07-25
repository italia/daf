package daf.stream

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
    id       = "",
    interval = 10,
    owner    = "",
    source   = null,
    sink     = null,
    schema   = schema
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