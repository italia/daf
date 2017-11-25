package it.gov.daf.datasetmanager

import catalog_manager.yaml.StorageHdfs
import it.gov.daf.catalogmanager._
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class DatasetManagerService(ws: WSClient)(implicit val ec: ExecutionContext) {

  private val baseCatalogUrl = ""
  private val catalogService = new Catalog_managerClient(ws)(baseCatalogUrl)

//  private val storageManager = new Storage_man


  def getDatasetSchema(authorization: String, datasetId: String): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)


    ???
  }

  def getDataset(authorization: String, datasetId: String): Future[Dataset] = {
    catalogService.datasetcatalogbyid(authorization, datasetId)

    ???
  }



  def getDataset(authorization: String, datasetId: String, numRows: Int): Future[Dataset] = {
    catalogService.datasetcatalogbyid(authorization, datasetId)
      .map(selectedDataset)


    ???

  }

  private def selectedDataset(catalog: MetaCatalog): Map[String, String] = ???

  def searchDataset(authorization: String, datasetId: String, query: Query): Future[Dataset] = {

    ???
  }
}
