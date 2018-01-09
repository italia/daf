/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package daf.dataset

import com.typesafe.config.Config
import it.teamdigitale.{DatasetOperations, PhysicalDatasetController}
import daf.catalogmanager.{CatalogManagerClient, MetaCatalog}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DatasetService(
  config: Config,
  ws: WSClient
)(implicit private val ec: ExecutionContext) {

  private val catalogClient = new CatalogManagerClient(config.getString("daf.catalog-url"))(ec)
  private val storageClient = PhysicalDatasetController(config)

  private val log = Logger(this.getClass)

  def schema(auth: String, uri: String): Future[StructType] = {
    catalogClient.datasetCatalogByUid(auth, uri)
      .map { mc =>
        log.debug(s"dataset catalog result $mc")
        mc
      }
      .flatMap(c => extractParamsF(c).map(_ + ("limit" -> "1")))
      .flatMap(params => Future.fromTry(storageClient.get(params)))
      .map(_.schema)
  }

  def data(auth: String, uri: String): Future[DataFrame] = {
    catalogClient.datasetCatalogByUid(auth, uri)
      .map { mc =>
        log.debug(s"dataset catalog result $mc")
        mc
      }
      .flatMap(extractParamsF)
      .flatMap(params => Future.fromTry(storageClient.get(params)))
  }

  def query(auth: String, uri: String, query: Query): Future[DataFrame] = {
    val result = catalogClient.datasetCatalogByUid(auth, uri)
      .map { mc =>
        log.debug(s"dataset catalog result $mc")
        mc
      }
      .flatMap(extractParamsF)
      .map(params => storageClient.get(params))
      .map { tryDf =>

        //applying select and where
        val df = for {
          selectDf <- DatasetOperations.select(tryDf, query.select.getOrElse(List.empty))
          whereDf <- DatasetOperations.where(Try(selectDf), query.where.getOrElse(List.empty))
        } yield whereDf

        //applying groupBy
        query.groupBy match {
          case Some(GroupBy(groupColumn, conditions)) =>
            val conditionsMap = conditions
              .map(c => c.column -> c.aggregationFunction)
            DatasetOperations.groupBy(df, groupColumn, conditionsMap: _*)
          case None => df
        }
      }

    //to flatten Future[Try[Df]] to Future[Df]
    result.flatMap {
      case Success(df) => Future.successful(df)
      case Failure(ex) => Future.failed(ex)
    }
  }

  private def extractParamsF(catalog: MetaCatalog): Future[Map[String, String]] =
    Future.fromTry(extractParams(catalog))

  private def extractParams(catalog: MetaCatalog): Try[Map[String, String]] = {
    catalog.operational.storage_info match {
      case Some(storage) =>
        if (storage.hdfs.isDefined) {
          Try(
            Map(
              "protocol" -> "hdfs",
              "path" -> s"${catalog.operational.physical_uri.get}" //storage.hdfs.flatMap(_.path).map(_ + "/final.parquet").get
            )
          )
        } else if (storage.kudu.isDefined) {
          Try(
            Map(
              "protocol" -> "kudu",
              "table" -> storage.kudu.map(_.name).get
            )
          )
        } //FIXME re enable after the merge
        //        } else if (storage.hbase.isDefined) {
        //          Try(
        //            Map(
        //              "protocol" -> "opentsdb",
        //              "metric" -> storage.hbase.flatMap(_.metric).get,
        //              //FIXME right now it encodes a list a as comma separated values of tags
        //              "tags" -> storage.hbase.flatMap(_.tags).get.mkString(","),
        //              //FIXME how to encode the interval?
        //              "interval" -> ""
        //            )
        //          )
        //        }
        else Failure(new IllegalArgumentException("no storage configured into catalog.operational field"))

      case None =>
        Failure(new IllegalArgumentException("no storage_info configured"))
    }
  }

}
