package it.gov.daf.iotingestion.common

final case class StorableEvent(version: Long, id: String, ts: Long, temporal_granularity: String, event_certainty: Double, event_type_id: Int, event_subtype_id: String, event_annotation: String, source: String, location: String, body: Array[Byte], attributesKeys: String, attributesValues: String)