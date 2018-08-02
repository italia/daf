package daf.stream.avro

import java.util.{ Date, Map => JavaMap }

import it.gov.daf.iot.event.{ Event, EventType, GenericValue, Location }
import org.scalacheck.Gen
import org.scalacheck.Gen._
import play.api.libs.json._

import scala.collection.convert.decorateAsJava._

object AvroGen {

  private val sourceGen = Gen.oneOf("domestic", "energy", "traffic")

  private val latitudeGen  = Gen.chooseNum[Double](6.7499552751, 18.4802470232)
  private val longitudeGen = Gen.chooseNum[Double](36.619987291, 47.1153931748)

  private val eventTypeGen = Gen.oneOf { EventType.values.toSeq }

  private val idGen = Gen.uuid.map { _.toString.toLowerCase }

  private val certaintyGen = Gen.chooseNum[Double](0, 1)

  private val subTypeOptGen = Gen.frequency(
    5  -> Some { "alarm" },
    5  -> Some { "warn" },
    90 -> None
  )

  private val eventAnnotationOptGen = Gen.frequency[Option[String]](
    1 -> Gen.chooseNum[Int](0, 1000).map { i => Some(s"Description [$i]") },
    1 -> None
  )

  private val attributePairGen = Gen.oneOf[(String, Any)](
    Gen.alphaLowerStr.map { s => "str-attr" -> s.take(20) },
    Gen.chooseNum[Int](-1000, 1000).map { "int-attr" -> _ },
    Gen.chooseNum[Long](-10000l, 10000l).map { "long-attr" -> _ },
    Gen.chooseNum[Double](-100d, 100d).map { "double-attr" -> _ },
    Gen.choose[Double](0d, 1d).map { _ >= 0.3 }.map { "boolean-attr" -> _ }
  )

  private val jsValueGen = attributePairGen.map[(String, JsValue)] {
    case (k, i: Int)     => k -> JsNumber(i)
    case (k, l: Long)    => k -> JsNumber(l)
    case (k, d: Double)  => k -> JsNumber(d)
    case (k, b: Boolean) => k -> JsBoolean(b)
    case (k, v)          => k -> JsString(v.toString)
  }

  private def mapGen(maxAttributes: Int) = for {
    size  <- Gen.chooseNum[Int](1, maxAttributes)
    pairs <- Gen.listOfN(size, attributePairGen)
  } yield pairs.zipWithIndex.map { case ((k, v), i) =>
    s"$k-$i" -> GenericValue.newBuilder.setValue(v).build()
  }.toMap[CharSequence, GenericValue].asJava

  private def jsonGen(maxAttributes: Int) = for {
    size  <- Gen.chooseNum[Int](1, maxAttributes)
    pairs <- Gen.listOfN(size, jsValueGen)
  } yield JsObject {
    pairs.zipWithIndex.map { case ((k, v), i) => s"$k-$i" -> v }
  }

  private val attributesMapGen = Gen.frequency[JavaMap[CharSequence, GenericValue]](
    7 -> mapGen(5),
    3 -> Map.empty[CharSequence, GenericValue].asJava
  )

  private val attributesStringGen = Gen.frequency[JsObject](
    7 -> jsonGen(5),
    3 -> JsObject { Seq.empty }
  ).map { Json.stringify }

  private val payloadMapGen = mapGen(15)

  private val payloadStringGen = jsonGen(15).map { Json.stringify }

  private val locationGen = for {
    latitude  <- latitudeGen
    longitude <- longitudeGen
  } yield Location.newBuilder.setLatitude(latitude).setLongitude(longitude).build

  private val locationOptGen = Gen.frequency[Option[Location]](
    3 -> Gen.some { locationGen },
    1 -> None
  )

  private val timestampGen = Gen.chooseNum[Long](-3600, 3600).map { new Date().getTime + _ }

  val event = for {
    id              <- idGen
    timestamp       <- timestampGen
    certainty       <- certaintyGen
    eventType       <- eventTypeGen
    subType         <- subTypeOptGen
    eventAnnotation <- eventAnnotationOptGen
    source          <- sourceGen
    location        <- locationOptGen
    payload         <- payloadStringGen
    attributes      <- attributesStringGen
  } yield Event.newBuilder
    .setVersion(1)
    .setId(id)
    .setTimestamp(timestamp)
    .setCertainty(certainty)
    .setType(eventType.name)
    .setSubtype { subType.orNull }
    .setEventAnnotation { eventAnnotation.orNull }
    .setSource(source)
    .setLatitude { location.map { _.getLatitude }.orNull }
    .setLongitude { location.map { _.getLongitude }.orNull }
    .setPayload(payload)
    .setAttributes(attributes)
    .build()

}
