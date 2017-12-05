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

  def dataschema(): Avro = {
    metaCatalog.dataschema.avro
  }

  def theme() = metaCatalog.operational.theme

  def subtheme() = metaCatalog.operational.subtheme

  def hdfsPath() = {
    val op = metaCatalog.operational
    dataset_typeNifi() match {
      case "standard" =>
        s"/daf/standard/${op.theme}/${op.subtheme}/${dsName()}"

      case "opendata" =>
        s"/daf/opendata/${dsName()}"

      case "ordinary" =>
        s"/daf/ordinary/${op.group_own}/${op.theme}/${op.subtheme}/${dsName()}"
    }
  }

  import it.gov.daf.catalogmanager.json._
  def avroSchema() = {
    val json = Json.toJson(metaCatalog.dataschema.avro)
    Json.stringify(json)
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

  /**
   * Methods to return default values based on MetaCatalog available info
   */
  def sourceSftpPathDefault(): String = {
    val sftpDefaultPrefix = sftpDefPrefix
    val theme = metaCatalog.operational.theme
    val subtheme = metaCatalog.operational.subtheme
    s"$sftpDefaultPrefix/${groupOwn()}/$theme/$subtheme/${dsName()}"
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

  /**
    *
    * @return the separator value extracted from
    *         "input_src": {
      "sftp": [
        {
          "name": "sftp_daf",
          "param": "format=csv, sep=;"
        }
      ]
    },
    */
  def separator() = {
    metaCatalog.operational
      .input_src.sftp
      .flatMap(_.headOption)
      .flatMap(_.param)
      .flatMap(_.split(", ").reverse.headOption)
      .map(_.replace("sep=", ""))
      .getOrElse(",")
  }

  def fileFormatNifi(): String = {
    val inputSftp = metaCatalog.operational.input_src.sftp

    inputSftp match {
      case Some(s) =>
        val sftps: Seq[SourceSftp] = s.filter(x => x.name.equals("sftp_daf"))
        if (sftps.nonEmpty) sftps.head.param.getOrElse("")
        else ""

      case None => ""
    }
  }

  def ingPipelineNifi(): String = {
    ingPipeline.mkString(",")
  }

}
