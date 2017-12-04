package it.gov.daf.server

//import dataset_manager.yaml._
import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import it.gov.daf.datasetmanager._
import it.gov.daf.server.storage.StorageManagerClient
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DatasetManagerService(
  catalogUrl: String,
  storageUrl: String,
  ws: WSClient
)(implicit val ec: ExecutionContext) {

  private val catalogService = new Catalog_managerClient(ws)(catalogUrl)
  private val storageManager = new StorageManagerClient(storageUrl, ws)

  def getDatasetSchema(authorization: String, datasetId: String): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .flatMap(c => Future.fromTry(extractParams(c)))
      .flatMap(storageManager.datasetSchema(authorization, _))

    result.map(_.as[Dataset])
  }

  def getDataset(authorization: String, datasetId: String): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .flatMap(c => Future.fromTry(extractParams(c)))
      .flatMap(storageManager.dataset(authorization, _))

    result.map(_.as[Dataset])
  }

  def getDataset(authorization: String, datasetId: String, numRows: Int): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .flatMap(c => Future.fromTry(extractParams(c)))
      .flatMap(storageManager.dataset(authorization, _))

    result.map(_.as[Dataset])
  }

  def searchDataset(authorization: String, datasetId: String, query: Query): Future[Dataset] = {
    val result = catalogService.datasetcatalogbyid(authorization, datasetId)
      .flatMap(c => Future.fromTry(extractParams(c)))
      .map(params => params ++ transform(query))
      .flatMap(storageManager.search(authorization, _))

    result.map(_.as[Dataset])
  }

  private def extractParams(catalog: MetaCatalog): Try[Map[String, String]] = {
    catalog.operational.storage_info match {
      case Some(storage) =>
        if (storage.hdfs.isDefined) {
          Try(
            Map(
              "protocol" -> "hdfs",
              "path" -> storage.hdfs.flatMap(_.path).get
            )
          )
        } else if (storage.kudu.isDefined) {
          Try(
            Map(
              "protocol" -> "kudu",
              "table" -> storage.kudu.flatMap(_.table_name).get
            )
          )
        } else if (storage.hbase.isDefined) {
          Try(
            Map(
              "protocol" -> "opentsdb",
              "metric" -> storage.hbase.flatMap(_.metric).get,
              //FIXME right now it encodes a list a as comma separated
              "tags" -> storage.hbase.flatMap(_.tags).get.mkString(","),
              //FIXME how to encode the interval?
              "interval" -> ""
            )
          )
        } else Failure(new IllegalArgumentException("no storage configured into catalog.operational field"))

      case None =>
        Failure(new IllegalArgumentException("no storage_info configured"))
    }
  }

  private def extractParams(catalog: MetaCatalog, numRows: Int = 100): Try[Map[String, String]] = {
    extractParams(catalog)
      .map(_ + ("limit" -> numRows.toString))
  }

  //FIXME this will be changed in the next sprint
  private def transform(query: Query): Map[String, String] = Map.empty
}
