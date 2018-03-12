package it.teamdigitale.events

import java.io.ByteArrayOutputStream

import com.twitter.bijection.Injection
import com.twitter.bijection.avro.{ GenericAvroCodecs, SpecificAvroCodecs }
import it.gov.daf.iotingestion.event.Event

import scala.util.Try

object SerializerDeserializer {

  val specificAvroBinaryInjection: Injection[Event, Array[Byte]] = SpecificAvroCodecs.toBinary[Event]

  def deserialize(bytes: Array[Byte]): Try[Event] = specificAvroBinaryInjection.invert(bytes)

  def serialize(event: Event): Array[Byte] = specificAvroBinaryInjection(event)



}
