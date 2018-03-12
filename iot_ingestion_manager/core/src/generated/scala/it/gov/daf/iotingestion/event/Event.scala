/** MACHINE-GENERATED FROM AVRO SCHEMA. DO NOT EDIT DIRECTLY */
package it.gov.daf.iotingestion.event

import scala.annotation.switch

/**
 * A generic event. See the reference guide for event format information.
 * @param version Version of this schema
 * @param id A globally unique identifier for this event.
 * @param ts Epoch timestamp in millis. Required.
 * @param temporal_granularity atom of time from a particular application’s point of view, for example: second, minute, hour, or day. Optional.
 * @param event_certainty estimation of the certainty of this particular event [0,1]. Optional.
 * @param event_type_id ID indicating the type of event: 0 for metric event, 1 for changing state events, 2 for generic events Required.
 * @param event_subtype_id It's an additional field that can be used to additionally qualify the event Optional.
 * @param event_annotation free-text explanation of what happened in this particular event. Optional.
 * @param source The event source attribute is the name of the entity that originated this event. This can be either an event producer or an event processing agent. Required
 * @param location Location from which the event was generated. Required.
 * @param body Raw event content in bytes. Optional.
 * @param attributes Event type-specific key/value pairs, usually extracted from the event body. Required.
 */
case class Event(var version: Long = 1L, var id: String, var ts: Long, var temporal_granularity: Option[String] = None, var event_certainty: Double = 1.0, var event_type_id: Int, var event_subtype_id: Option[String] = None, var event_annotation: Option[String] = None, var source: String, var location: String = "", var body: Option[Array[Byte]] = None, var attributes: Map[String, String]) extends org.apache.avro.specific.SpecificRecordBase {
  def this() = this(1L, "", 0L, None, 1.0, 0, None, None, "", "", None, Map("" -> ""))
  def get(field$: Int): AnyRef = {
    (field$: @switch) match {
      case 0 => {
        version
      }.asInstanceOf[AnyRef]
      case 1 => {
        id
      }.asInstanceOf[AnyRef]
      case 2 => {
        ts
      }.asInstanceOf[AnyRef]
      case 3 => {
        temporal_granularity match {
          case Some(x) => x
          case None => null
        }
      }.asInstanceOf[AnyRef]
      case 4 => {
        event_certainty
      }.asInstanceOf[AnyRef]
      case 5 => {
        event_type_id
      }.asInstanceOf[AnyRef]
      case 6 => {
        event_subtype_id match {
          case Some(x) => x
          case None => null
        }
      }.asInstanceOf[AnyRef]
      case 7 => {
        event_annotation match {
          case Some(x) => x
          case None => null
        }
      }.asInstanceOf[AnyRef]
      case 8 => {
        source
      }.asInstanceOf[AnyRef]
      case 9 => {
        location
      }.asInstanceOf[AnyRef]
      case 10 => {
        body match {
          case Some(x) => java.nio.ByteBuffer.wrap(x)
          case None => null
        }
      }.asInstanceOf[AnyRef]
      case 11 => {
        val map: java.util.HashMap[String, Any] = new java.util.HashMap[String, Any]
        attributes foreach { kvp =>
          val key = kvp._1
          val value = kvp._2
          map.put(key, value)
        }
        map
      }.asInstanceOf[AnyRef]
      case _ => new org.apache.avro.AvroRuntimeException("Bad index")
    }
  }
  def put(field$: Int, value: Any): Unit = {
    (field$: @switch) match {
      case 0 => this.version = {
        value
      }.asInstanceOf[Long]
      case 1 => this.id = {
        value.toString
      }.asInstanceOf[String]
      case 2 => this.ts = {
        value
      }.asInstanceOf[Long]
      case 3 => this.temporal_granularity = {
        value match {
          case null => None
          case _ => Some(value.toString)
        }
      }.asInstanceOf[Option[String]]
      case 4 => this.event_certainty = {
        value
      }.asInstanceOf[Double]
      case 5 => this.event_type_id = {
        value
      }.asInstanceOf[Int]
      case 6 => this.event_subtype_id = {
        value match {
          case null => None
          case _ => Some(value.toString)
        }
      }.asInstanceOf[Option[String]]
      case 7 => this.event_annotation = {
        value match {
          case null => None
          case _ => Some(value.toString)
        }
      }.asInstanceOf[Option[String]]
      case 8 => this.source = {
        value.toString
      }.asInstanceOf[String]
      case 9 => this.location = {
        value.toString
      }.asInstanceOf[String]
      case 10 => this.body = {
        value match {
          case null => None
          case _ => Some(value match {
            case (buffer: java.nio.ByteBuffer) => {
              buffer.array()
            }
          })
        }
      }.asInstanceOf[Option[Array[Byte]]]
      case 11 => this.attributes = {
        value match {
          case (map: java.util.Map[_,_]) => {
            scala.collection.JavaConverters.mapAsScalaMapConverter(map).asScala.toMap map { kvp =>
              val key = kvp._1.toString
              val value = kvp._2
              (key, value.toString)
            }
          }
        }
      }.asInstanceOf[Map[String, String]]
      case _ => new org.apache.avro.AvroRuntimeException("Bad index")
    }
    ()
  }
  def getSchema: org.apache.avro.Schema = Event.SCHEMA$
}

object Event {
  val SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Event\",\"namespace\":\"it.gov.daf.iotingestion.event\",\"doc\":\"A generic event. See the reference guide for event format information.\",\"fields\":[{\"name\":\"version\",\"type\":\"long\",\"doc\":\"Version of this schema\",\"default\":1},{\"name\":\"id\",\"type\":\"string\",\"doc\":\"A globally unique identifier for this event.\"},{\"name\":\"ts\",\"type\":\"long\",\"doc\":\"Epoch timestamp in millis. Required.\"},{\"name\":\"temporal_granularity\",\"type\":[\"null\",\"string\"],\"doc\":\"atom of time from a particular application’s point of view, for example: second, minute, hour, or day. Optional.\",\"default\":null},{\"name\":\"event_certainty\",\"type\":\"double\",\"doc\":\"estimation of the certainty of this particular event [0,1]. Optional.\",\"default\":1.0},{\"name\":\"event_type_id\",\"type\":\"int\",\"doc\":\"ID indicating the type of event: 0 for metric event, 1 for changing state events, 2 for generic events Required.\"},{\"name\":\"event_subtype_id\",\"type\":[\"null\",\"string\"],\"doc\":\"It\'s an additional field that can be used to additionally qualify the event Optional.\",\"default\":null},{\"name\":\"event_annotation\",\"type\":[\"null\",\"string\"],\"doc\":\"free-text explanation of what happened in this particular event. Optional.\",\"default\":null},{\"name\":\"source\",\"type\":\"string\",\"doc\":\"The event source attribute is the name of the entity that originated this event. This can be either an event producer or an event processing agent. Required\"},{\"name\":\"location\",\"type\":\"string\",\"doc\":\"Location from which the event was generated. Required.\",\"default\":\"\"},{\"name\":\"body\",\"type\":[\"null\",\"bytes\"],\"doc\":\"Raw event content in bytes. Optional.\",\"default\":null},{\"name\":\"attributes\",\"type\":{\"type\":\"map\",\"values\":\"string\"},\"doc\":\"Event type-specific key/value pairs, usually extracted from the event body. Required.\",\"order\":\"ignore\"}],\"version\":1}")
}