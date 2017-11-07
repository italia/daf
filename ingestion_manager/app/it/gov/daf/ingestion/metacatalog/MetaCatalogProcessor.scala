package it.gov.daf.ingestion.metacatalog

import com.typesafe.config.ConfigFactory
import play.api.libs.json._
import it.gov.daf.catalogmanager._
import it.gov.daf.catalogmanager.json._
import org.slf4j.{Logger, LoggerFactory}
import org.apache.commons.lang.StringEscapeUtils

//Get Logical_uri, process MetadataCatalog and get the required info
class MetaCatalogProcessor(metaCatalog: MetaCatalog) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val sftpDefPrefix = ConfigFactory.load().getString("ingmgr.sftpdef.prefixdir")


  /*
  Basic call to catalog manager
   */

  def dsName(): String = {
    metaCatalog.dcatapit.name
  }

  def inputSrc(): InputSrc = {
    metaCatalog.operational.input_src
  }

  def storage(): StorageInfo = {

    metaCatalog.operational.storage_info match {
      case Some(s) => s
      case None => MetaCatalogDefault.storageInfo()
    }
  }

  def ingPipeline(): List[String] = {

    metaCatalog.operational.ingestion_pipeline match {
      case Some(s) => s
      case None => MetaCatalogDefault.ingPipeline()
    }
  }

  def dataschema(): Avro ={

    metaCatalog.dataschema.avro

  }

  def dsType(): String = {
    metaCatalog.operational.dataset_type
  }

  def groupAccess(): List[GroupAccess] = {

    metaCatalog.operational.group_access match {
      case Some(s) => s
      case None => List()
    }
  }

  def groupOwn(): String = metaCatalog.operational.group_own

  //def opsTheme(): String = meta


  /*

  Methods to return default values based on MetaCatalog available info

   */

  def sourceSftpPathDefault(sftpName: String): String = {

    val sftpDefaultPrefix = sftpDefPrefix
    val theme = metaCatalog.operational.theme
    val subtheme = metaCatalog.operational.subtheme

    sftpDefaultPrefix + "/" + groupOwn + "/" + theme + "/" + subtheme
  }




  /*

  Chiamate per ritornare info specifiche per NiFi

   */

  def inputSrcNifi(): String = {

    StringEscapeUtils.escapeJava(Json.toJson(inputSrc()).toString)

  }

  def storageNifi(): String = {

    StringEscapeUtils.escapeJava(Json.toJson(storage()).toString)

  }

  def dataschemaNifi(): String = {

    StringEscapeUtils.escapeJava(Json.toJson(dataschema()).toString)

  }

  def dataset_typeNifi(): String = {
    val datasetType: String = dsType()
    val isStd: Boolean = metaCatalog.operational.is_std

    if (isStd) {
      "standard"
    } else {
      if (dsType.contains("opendata")) {
        "opendata"
      } else {
        "ordinary"
      }
    }
  }

  def fileFormatNifi(): String ={
    val inputSftp = metaCatalog.operational.input_src.sftp

    inputSftp match {
      case Some(s) =>
        val sftps: Seq[SourceSftp] = s.filter(x=>x.name.equals("sftp_daf"))
        if (sftps.length>0) {
          sftps(0).param.getOrElse("")
        } else {
          ""
        }
    }

  }

  def ingPipelineNifi(): String = {
    ingPipeline.mkString(",")
  }



}
