package it.teamdigitale

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

import it.gov.daf.iotingestion.event.Event
import org.apache.logging.log4j.LogManager

import scala.util.Try

package object EventModel {

  val alogger = LogManager.getLogger(this.getClass)

  case class StorableEvent(version: Long,
                           id: String,
                           ts: Long,
                           temporal_granularity: String,
                           event_certainty: Double,
                           event_type_id: Int,
                           event_subtype_id: Option[String],
                           event_annotation: String,
                           source: String,
                           location: String,
                           body: Array[Byte],
                           attributesKeys: String,
                           attributesValues: String,
                           attributes: Map[String, String]
                          )

  case class KuduEvent(version: Long,
                       id: String,
                       ts: Long,
                       temporal_granularity: String,
                       event_certainty: Double,
                       event_type_id: Int,
                       metric_id: String,
                       event_annotation: String,
                       source: String,
                       location: String,
                       metric: Double,
                       attributesKeys: String,
                       attributesValues: String
                      )


  case class HdfsEvent(
                        version: Long,
                        id: String,
                        ts: Long,
                        day: String,
                        temporal_granularity: String,
                        event_certainty: Double,
                        event_type_id: Int,
                        event_subtype_id: String,
                        event_annotation: String,
                        metric: Option[Double],
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
          event_subtype_id = a.event_subtype_id,
          event_annotation = a.event_annotation.getOrElse(null),
          source = a.source,
          location = a.location,
          body = a.body.getOrElse(null),
          attributesKeys = a.attributes.keys.mkString("#"),
          attributesValues = a.attributes.values.mkString("#"),
          attributes = a.attributes
        )

      )

    }
  }


  /**
    * It converts a StorableEvent into a HdfsEvent
    */
  object EventToHdfsEvent {

    def apply(a:StorableEvent): Try[HdfsEvent] = Try {
      HdfsEvent(
        version = a.version,
        id = a.id,
        ts = a.ts,
        day = new SimpleDateFormat("ddMMyyyy").format(new Date(a.ts)),
        temporal_granularity = a.temporal_granularity,
        event_certainty = a.event_certainty,
        event_type_id = a.event_type_id,
        event_subtype_id = a.event_subtype_id.orNull,
        event_annotation = a.event_annotation,
        metric = a.attributes.get("value").map(_.asInstanceOf[Double]),
        source = a.source,
        location = a.location,
        body = a.body,
        attributesKeys = a.attributesKeys,
        attributesValues = a.attributesValues
      )
    }



  }

  /**
    * It converts a StorableEvent into a HdfsEvent
    */
  object EventToKuduEvent {

    def apply(a: StorableEvent): Try[KuduEvent] = {
      Try {

        val m: Double = a.attributes.get("value").flatMap(m => Try{m.toDouble}.toOption) match {
          case Some(v) => v
          case None => alogger.error(s"The event $a doesn't have either the value field or it cannot be casted into Double")
            throw new RuntimeException(s"The event $a doesn't have either the value field or it cannot be casted into Double")
        }

        if(a.event_subtype_id.isEmpty) {
          alogger.error(s"Event $a doesn't have a metric id")
          throw new RuntimeException("No metric id stored")
        }


        KuduEvent(
          version = a.version,
          id = a.id,
          ts = a.ts,
          temporal_granularity = a.temporal_granularity,
          event_certainty = a.event_certainty,
          event_type_id = a.event_type_id,
          metric_id = a.event_subtype_id.get,
          event_annotation = a.event_annotation,
          metric = m,
          source = a.source,
          location = a.location,
          attributesKeys = a.attributesKeys,
          attributesValues = a.attributesValues
        )
      }
    }
  }

}
