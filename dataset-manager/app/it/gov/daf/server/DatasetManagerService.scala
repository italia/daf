package it.gov.daf.server

import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import play.api.libs.ws.WSClient
import dataset_manager.yaml._

import scala.concurrent.{ExecutionContext, Future}

class DatasetManagerService(
  catalogUrl: String,
  storageUrl: String,
  ws: WSClient
)(implicit val ec: ExecutionContext) {

  private val catalogService = new Catalog_managerClient(ws)(catalogUrl)
  private val storageManager = new StorageManagerClient(storageUrl, ws)

  def getDatasetSchema(authorization: String, datasetId: String): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .map(extractParams)
      .flatMap(storageManager.datasetSchema(authorization, _))

    result.map(_.as[Dataset])
  }

  def getDataset(authorization: String, datasetId: String): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .map(extractParams)
      .flatMap(storageManager.dataset(authorization, _))

    result.map(_.as[Dataset])
  }

  def getDataset(authorization: String, datasetId: String, numRows: Int): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .map(extractParams(_, numRows))
      .flatMap(storageManager.dataset(authorization, _))

    result.map(_.as[Dataset])
  }

  def searchDataset(authorization: String, datasetId: String, query: Query): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .map(extractParams)
      .map(params => params ++ transform(query))
      .flatMap(storageManager.search(authorization, _))

    result.map(_.as[Dataset])
  }

  private def extractParams(catalog: MetaCatalog): Map[String, Any] = ???

  private def extractParams(catalog: MetaCatalog, numRows: Int = 100): Map[String, Any] = ???

  private def transform(query: Query): Map[String, Any] = ???
}
