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

package controllers

import akka.stream.scaladsl.Source
import cats.syntax.show.toShow
import cats.syntax.traverse.toTraverseOps
import cats.instances.option.catsStdInstancesForOption
import cats.instances.try_.catsStdInstancesForTry
import cats.instances.map.catsStdInstancesForMap
import cats.instances.list.catsStdInstancesForList
import daf.dataset._
import daf.dataset.query.jdbc.{ JdbcResult, QueryFragmentWriterSyntax, Writers }
import daf.dataset.query.Query
import daf.web._
import daf.filesystem._
import daf.instances.FileSystemInstance
import it.gov.daf.common.utils._
import org.apache.hadoop.fs.Path
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

trait QueryExecution { this: DatasetController with DatasetExport with FileSystemInstance =>

  private def extractDatabaseName(parent: String, params: FileDatasetParams) = parent.toLowerCase match {
    case "opendata" => params.extraParams.get("theme").map { s => s"opendata__${s.toLowerCase}" } getOrElse "opendata" // append __{theme} for opendata
    case other      => other // use the parent dir for other data
  }

  private def extractTableName(path: Path, params: FileDatasetParams): Try[String] = Try {
    s"${extractDatabaseName(path.getParent.getName, params)}.${path.getName.toLowerCase}"
  }

  protected def extractTableName(params: DatasetParams, userId: String): Try[String] = params match {
    case kudu: KuduDatasetParams => (proxyUser as userId) { downloadService.tableInfo(kudu.table) } map { _ => kudu.table }
    case file: FileDatasetParams => (proxyUser as userId) { extractTableName(file.path.asHadoop.resolve, file) }
  }

  private def extractTableMap(params: List[DatasetParams], userId: String) = params.map { p => p.catalogUri -> p }.traverse[Try, (String, String)] {
    case (k, p) => extractTableName(p, userId).map { k -> _ }
  }

  private def prepareQuery(params: DatasetParams, otherParams: List[DatasetParams], query: Query, userId: String) = for {
    tableName <- extractTableName(params, userId)
    tableRef  <- extractTableMap(otherParams, userId)
    fragment  <- Writers.sql(query, tableName, tableRef.toMap).write
  } yield fragment.query[Unit].sql

  private def analyzeQuery(params: DatasetParams, otherParams: List[DatasetParams], query: Query, userId: String) = for {
    tableName <- extractTableName(params, userId)
    tableRef  <- extractTableMap(otherParams, userId)
    analysis  <- queryService.explain(query, tableName, tableRef.toMap, userId)
  } yield analysis

  private def transform(jdbcResult: JdbcResult, targetFormat: FileDataFormat) = targetFormat match {
    case CsvFileFormat  => Try {
      Source[String](jdbcResult.toCsv).map { csv => s"$csv${System.lineSeparator}" }
    }
    case JsonFileFormat => Try {
      wrapJson {
        Source[JsValue](jdbcResult.toJson).map { _.toString }
      }
    }
    case _              => Failure { new IllegalArgumentException(s"Invalid target format [$targetFormat]; must be [csv | json]") }
  }

  // Web
  // Failure

  private def failQuickExec(params: DatasetParams, targetFormat: FileDataFormat) = Future.successful {
    TemporaryRedirect {
      s"${controllers.routes.DatasetController.queryDataset(params.catalogUri, targetFormat.show, "batch").url}"
    }
  }

  // Executions

  private def doBatchExec(params: DatasetParams, otherParams: List[DatasetParams], query: Query, targetFormat: FileDataFormat, userId: String) = prepareQuery(params, otherParams, query, userId) match {
    case Success(sql)   => prepareQueryExport(sql, targetFormat).map { formatExport(_, targetFormat) }
    case Failure(error) => Future.failed { error }
  }

  private def doQuickExec(params: DatasetParams, otherParams: List[DatasetParams], query: Query, targetFormat: FileDataFormat, userId: String) = for {
    tableName  <- extractTableName(params, userId)
    tableRef   <- extractTableMap(otherParams, userId)
    jdbcResult <- queryService.exec(query, tableName, tableRef.toMap, userId)
    data       <- transform(jdbcResult, targetFormat)
  } yield data

  // API

  protected def quickExec(params: DatasetParams, otherParams: List[DatasetParams], query: Query, targetFormat: FileDataFormat, userId: String) = analyzeQuery(params, otherParams, query, userId) match {
    case Success(analysis) if analysis.memoryEstimation <= impalaConfig.memoryEstimationLimit => doQuickExec(params, otherParams, query, targetFormat, userId).~>[Future].map { respond(_, params.name, targetFormat) }
    case Success(_)                                                                           => failQuickExec(params, targetFormat)
    case Failure(error)                                                                       => Future.failed { error }
  }

  protected def batchExec(params: DatasetParams, otherParams: List[DatasetParams], query: Query, targetFormat: FileDataFormat, userId: String) =
    doBatchExec(params, otherParams, query, targetFormat, userId).map { respond(_, params.name, targetFormat) }

  protected def exec(params: DatasetParams, otherParams: List[DatasetParams], query: Query, userId: String, targetFormat: FileDataFormat, method: DownloadMethod) = method match {
    case QuickDownloadMethod => quickExec(params, otherParams, query, targetFormat, userId)
    case BatchDownloadMethod => batchExec(params, otherParams, query, targetFormat, userId)
  }

}