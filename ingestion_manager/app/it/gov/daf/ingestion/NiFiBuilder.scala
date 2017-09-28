package it.gov.daf.ingestion

class NiFiBuilder(metaCatalogProcessor: MetaCatalogProcessor) {

  def getNiFiInfo(): NiFiInfo = {
    val dsName = metaCatalogProcessor.getDsName()
    val ingSource = metaCatalogProcessor.getDsName()
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
