package it.gov.daf.server

import play.api.libs.json.JsValue

import scala.concurrent.Future

trait StorageManagerTrait {

  /**
    *
    * @param authorization
    * @param params
    *
    *               - for hdfs => Map(
                      "protocol" -> "hdfs",
                      "path" -> super.pathParquet,
                      "format" -> "parquet"
                    )

                    - for opentsdb => Map(
                        "protocol" -> "opentsdb",
                          "metric" -> super.metric
                        )

                    - for kudu ???


    * @return
    */
  def dataset(authorization: String, params: Map[String, Any]): Future[JsValue]

  def datasetSchema(authorization: String, params: Map[String, Any]): Future[JsValue]

  def search(authorization: String, params: Map[String, Any]): Future[JsValue]
}
