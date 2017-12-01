package it.gov.daf.ingestion.pipelines.data

import ingestion_manager.yaml.PipelineInfo
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

object PipelineClass {

  implicit val pipelineInfoReads: Reads[PipelineInfo] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "id").read[String] and
      (JsPath \ "default_stream").read[Boolean] and
      (JsPath \ "category").read[String] and
      (JsPath \ "default_batch").read[Boolean]

    )(PipelineInfo.apply _)

}
