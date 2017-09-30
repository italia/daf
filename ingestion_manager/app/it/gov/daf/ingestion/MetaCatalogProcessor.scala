package it.gov.daf.ingestion

import it.gov.daf.catalogmanager.{Dataset, MetaCatalog}
import org.slf4j.{ Logger, LoggerFactory }

//Get Logical_uri, process MetadataCatalog and get the required info
class MetaCatalogProcessor(metaCatalog: MetaCatalog, logicalUri: String) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  //Get Avro - Dataset Name
  def getAvroDsName(): String = {

    val datasetName = metaCatalog.dataschema.avro.name
    //logger.error(s"Dataset Name not found on $logicalUri")

    datasetName
  }

  //Get DCATAPIT - Dataset Name
  def getDsName(): String = {
    val datasetName: String = metaCatalog.dcatapit.name
    datasetName
  }



}
