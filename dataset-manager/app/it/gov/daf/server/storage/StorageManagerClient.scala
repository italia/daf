package it.gov.daf.server.storage

import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  * This class calls the storage manager as a service
  * @param basePath
  * @param ws
  * @param ec
  */
class StorageManagerClient(basePath: String, ws: WSClient)(implicit ec: ExecutionContext)
  extends StorageManagerTrait {

  def dataset(authorization: String, params: Map[String, Any]): Future[JsValue] = ???

  def datasetSchema(authorization: String, params: Map[String, Any]): Future[JsValue] = ???

  def search(authorization: String, params: Map[String, Any]): Future[JsValue] = ???
}
