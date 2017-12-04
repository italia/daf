package it.gov.daf.server

import it.gov.daf.datasetmanager._

import play.api.libs.json._

package object json {
  implicit lazy val QueryReads: Reads[Query] = Reads[Query] {
    json => JsSuccess(Query((json \ "filter").asOpt[List[String]], (json \ "where").asOpt[List[String]], (json \ "group_by").asOpt[GroupBy], (json \ "limit").asOpt[Int]))
  }
  implicit lazy val QueryWrites: Writes[Query] = Writes[Query] {
    o => JsObject(Seq("filter" -> Json.toJson(o.filter), "where" -> Json.toJson(o.where), "group_by" -> Json.toJson(o.group_by), "limit" -> Json.toJson(o.limit)).filter(_._2 != JsNull))
  }
  implicit lazy val GroupByReads: Reads[GroupBy] = Reads[GroupBy] {
    json => JsSuccess(GroupBy((json \ "groupColumn").asOpt[String], (json \ "conditions").asOpt[List[GroupCondition]]))
  }
  implicit lazy val GroupByWrites: Writes[GroupBy] = Writes[GroupBy] {
    o => JsObject(Seq("groupColumn" -> Json.toJson(o.groupColumn), "conditions" -> Json.toJson(o.conditions)).filter(_._2 != JsNull))
  }
  implicit lazy val GroupConditionReads: Reads[GroupCondition] = Reads[GroupCondition] {
    json => JsSuccess(GroupCondition((json \ "column").asOpt[String], (json \ "aggrFunction").asOpt[String]))
  }
  implicit lazy val GroupConditionWrites: Writes[GroupCondition] = Writes[GroupCondition] {
    o => JsObject(Seq("column" -> Json.toJson(o.column), "aggrFunction" -> Json.toJson(o.aggrFunction)).filter(_._2 != JsNull))
  }
  implicit lazy val DatasetReads: Reads[Dataset] = Reads[Dataset] {
    json => JsSuccess(Dataset((json \ "datasetId").asOpt[String], (json \ "columns").asOpt[List[Column]], (json \ "rows").asOpt[List[String]]))
  }
  implicit lazy val DatasetWrites: Writes[Dataset] = Writes[Dataset] {
    o => JsObject(Seq("datasetId" -> Json.toJson(o.datasetId), "columns" -> Json.toJson(o.columns), "rows" -> Json.toJson(o.rows)).filter(_._2 != JsNull))
  }
  implicit lazy val ColumnReads: Reads[Column] = Reads[Column] {
    json => JsSuccess(Column((json \ "name").asOpt[String], (json \ "data_type").asOpt[String], (json \ "description").asOpt[String]))
  }
  implicit lazy val ColumnWrites: Writes[Column] = Writes[Column] {
    o => JsObject(Seq("name" -> Json.toJson(o.name), "data_type" -> Json.toJson(o.data_type), "description" -> Json.toJson(o.description)).filter(_._2 != JsNull))
  }
  implicit lazy val ErrorReads: Reads[Error] = Reads[Error] {
    json => JsSuccess(Error((json \ "code").asOpt[Int], (json \ "message").asOpt[String], (json \ "fields").asOpt[String]))
  }
  implicit lazy val ErrorWrites: Writes[Error] = Writes[Error] {
    o => JsObject(Seq("code" -> Json.toJson(o.code), "message" -> Json.toJson(o.message), "fields" -> Json.toJson(o.fields)).filter(_._2 != JsNull))
  }
}