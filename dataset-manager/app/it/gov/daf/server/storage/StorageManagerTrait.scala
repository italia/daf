package it.gov.daf.server.storage

import play.api.libs.json.JsValue
import scala.concurrent.Future

trait StorageManagerTrait {

  def dataset(authorization: String, params: Map[String, Any]): Future[JsValue]

  def datasetSchema(authorization: String, params: Map[String, Any]): Future[JsValue]

  def search(authorization: String, params: Map[String, Any]): Future[JsValue]
}
