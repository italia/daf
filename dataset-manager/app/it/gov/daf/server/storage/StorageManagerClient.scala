package it.gov.daf.server.storage

import it.gov.daf.server.StorageManagerTrait
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class StorageManagerClient(basePath: String, ws: WSClient)(implicit ec: ExecutionContext)
  extends StorageManagerTrait {

  def dataset(authorization: String, params: Map[String, Any]): Future[JsValue] = ???

  def datasetSchema(authorization: String, params: Map[String, Any]): Future[JsValue] = ???

  def search(authorization: String, params: Map[String, Any]): Future[JsValue] = ???
}
