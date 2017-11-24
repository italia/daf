package it.gov.daf.ingestion.pipelines

import java.io.FileInputStream
import com.typesafe.config.ConfigFactory
import ingestion_manager.yaml.PipelineInfo
import play.api.libs.json._
import data.PipelineClass.pipelineInfoReads

object PipelineInfoRead {
  val pipeInfoFile = ConfigFactory.load().getString("ingmgr.pipeinfo.datapath")

  def pipelineInfo(): List[PipelineInfo] = {
    val stream = new FileInputStream(pipeInfoFile)
    val pipeInfoOpt: Option[List[PipelineInfo]] = try { Json.parse(stream).asOpt[List[PipelineInfo]] } finally { stream.close() }
    pipeInfoOpt match {
      case Some(s) => s
      case None => List()
    }
  }

  def pipelineInfoByCat(category: String): List[PipelineInfo] = {
    val pipelineList = pipelineInfo()
    pipelineList.filter(_.category.equals(category))
  }
  def pipelineInfoById(id: String): List[PipelineInfo] = {
    val pipelineList = pipelineInfo()
    pipelineList.filter(_.id.equals(id))
  }

}

