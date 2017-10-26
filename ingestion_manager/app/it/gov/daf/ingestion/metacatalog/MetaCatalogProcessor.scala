package it.gov.daf.ingestion.metacatalog

import it.gov.daf.catalogmanager._
import org.slf4j.{Logger, LoggerFactory}

//Get Logical_uri, process MetadataCatalog and get the required info
class MetaCatalogProcessor(metaCatalog: MetaCatalog) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)


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


  /*

  Chiamate per ritornare info specifiche per NiFi

   */






}
