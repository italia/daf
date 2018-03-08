package it.teamdigitale

import java.util.concurrent.TimeUnit

import it.gov.daf.iotingestion.event.Event
import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs

import scala.util.Try

package object EventModel {


  case class StorableEvent(version: Long,
                           id: String,
                           ts: Long,
                           temporal_granularity: String,
                           event_certainty: Double,
                           event_type_id: Int,
                           event_subtype_id: String,
                           event_annotation: String,
                           source: String,
                           location: String,
                           body: Array[Byte],
                           attributesKeys: String,
                           attributesValues: String
                          )


  case class StorableGenericEvent(
                                   version: Long,
                                   id: String,
                                   ts: Long,
                                   day: Long,
                                   temporal_granularity: String,
                                   event_certainty: Double,
                                   event_type_id: Int,
                                   event_subtype_id: String,
                                   event_annotation: String,
                                   source: String,
                                   location: String,
                                   body: Array[Byte],
                                   attributesKeys: String,
                                   attributesValues: String
                                 )


  object EventToStorableEvent {
    def apply(a: Try[Event]): Try[StorableEvent] =  {
      a.map( a =>
        StorableEvent(
          version = a.version,
          id = a.id,
          ts = a.ts,
          temporal_granularity = a.temporal_granularity.getOrElse(null),
          event_certainty = a.event_certainty,
          event_type_id = a.event_type_id,
          event_subtype_id = a.event_subtype_id.getOrElse(null),
          event_annotation = a.event_annotation.getOrElse(null),
          source = a.source,
          location = a.location,
          body = a.body.getOrElse(null),
          attributesKeys = a.attributes.keys.mkString("#"),
          attributesValues = a.attributes.values.mkString("#")
        )

      )

    }
  }


  /**
    * It converts a StorableEvent into a StorableGenericEvent
    */
  object EventToStorableGenericEvent {

    def apply(a: StorableEvent): StorableGenericEvent =  {
      StorableGenericEvent(
        version = a.version,
        id = a.id,
        ts = a.ts,
        day = new SimpleDateFormat("ddMMyyyy").format(new Date(a.ts)),
        temporal_granularity = a.temporal_granularity,
        event_certainty = a.event_certainty,
        event_type_id = a.event_type_id,
        event_subtype_id = a.event_subtype_id,
        event_annotation = a.event_annotation,
        source = a.source,
        location = a.location,
        body = a.body,
        attributesKeys = a.attributesKeys,
        attributesValues = a.attributesValues
      )
    }


  }

}
