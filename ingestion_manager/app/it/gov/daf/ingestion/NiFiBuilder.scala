package it.gov.daf.ingestion

import it.gov.daf.catalogmanager.MetaCatalog

//class NiFiBuilder(metaCatalogProcessor: MetaCatalogProcessor) {
class NiFiBuilder(metaCatalog: MetaCatalog) {

  def getNiFiInfo(): NiFiInfo = {
    val dsName = metaCatalog.dcatapit.name
    //val inputSrc = ???
    //val storage = ???
    //val ingPipeline = ???
    //val dsType = ???
    //val groupAccess = ???


    NiFiInfo(dsName)
  }

  def processorBuilder(): NiFiProcessStatus = {
    val niFiInfo: NiFiInfo = getNiFiInfo()
    //Call NiFi API to setup a new processor
    NiFiProcessStatus(niFiInfo)
  }



}

case class NiFiInfo(dsName: String)
case class NiFiProcessStatus(niFiInfo: NiFiInfo)
