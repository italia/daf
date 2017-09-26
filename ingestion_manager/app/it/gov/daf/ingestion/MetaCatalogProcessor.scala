package it.gov.daf.ingestion

import it.gov.daf.catalogmanager.{Dataset, MetaCatalog}
import org.slf4j.{ Logger, LoggerFactory }

//Get Logical_uri, process MetadataCatalog and get the required info
class MetaCatalogProcessor(metaCatalog: MetaCatalog, logicalUri: String) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  //Get Avro - Dataset Name
  def getAvroDsName(): String = {
    val datasetName: String = metaCatalog.dataschema match {
      case Some(dataschema) => {
        dataschema.avro match {
          case Some(avro) => avro.name
          case _ => {
            logger.error(s"Dataset Name not found on $logicalUri")
            "na"
          }
        }
      }
      case None => {
        logger.error(s"Dataset Name not found on $logicalUri")
        "na"
      }
    }
    datasetName
  }

  //Get Avro - Dataset Name
  def getDsName(): String = {
    val datasetName: String = metaCatalog.dcatapit match {
      case Some(dcatapit) => dcatapit.name
      case None => {
        logger.error(s"Dataset Name not found on $logicalUri")
        "na"
      }
    }
    datasetName
  }

}
