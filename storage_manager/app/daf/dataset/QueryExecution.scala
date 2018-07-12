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

import akka.stream.scaladsl.{ Source, StreamConverters }
import cats.syntax.show.toShow
import controllers.DatasetController
import daf.dataset.query.jdbc.{ JdbcResult, QueryFragmentWriterSyntax, Writers }
import daf.dataset.query.Query
import daf.web._
import daf.filesystem._
import it.gov.daf.common.utils._
import org.apache.hadoop.fs.Path
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

trait QueryExecution { this: DatasetController =>

  private def extractTableName(path: Path): Try[String] = Try { s"${path.getParent.getName}.${path.getName}" }

  private def extractTableName(params: DatasetParams, userId: String): Try[String] = params match {
    case kudu: KuduDatasetParams => (proxyUser as userId) { downloadService.tableInfo(kudu.table) } map { _ => kudu.table }
    case file: FileDatasetParams => (proxyUser as userId) { extractTableName(file.path.asHadoop.resolve) }
  }

  private def prepareQuery(params: DatasetParams, query: Query, userId: String) = for {
    tableName <- extractTableName(params, userId)
    fragment  <- Writers.sql(query, tableName).write
  } yield fragment.query[Unit].sql

  private def analyzeQuery(params: DatasetParams, query: Query, userId: String) = for {
    tableName <- extractTableName(params, userId)
    analysis  <- queryService.explain(query, tableName, userId)
  } yield analysis

  private def prepareQueryExport(query: String, targetFormat: FileDataFormat) =
    fileExportService.exportQuery(query, targetFormat).map { downloadService.openPath }.flatMap {
      case Success(stream) => Future.successful {
        StreamConverters.fromInputStream { () => stream }
      }
      case Failure(error)  => Future.failed { error }
    }

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
  // Success

  private def respond(params: DatasetParams, data: Source[String, _], targetFormat: FileDataFormat) = Ok.chunked(data).withHeaders(
    CONTENT_DISPOSITION -> s"""attachment; filename="${params.name}.${targetFormat.show}"""",
    CONTENT_TYPE        -> contentType(targetFormat)
  )

  // Failure

  private def failQuickExec(params: DatasetParams, targetFormat: FileDataFormat) = Future.successful {
    TemporaryRedirect {
      s"${controllers.routes.DatasetController.queryDataset(params.catalogUri, targetFormat.show).url}?format=${targetFormat.show}&method=batch"
    }
  }


  // Executions

  private def doBatchExec(params: DatasetParams, query: Query, targetFormat: FileDataFormat, userId: String) = prepareQuery(params, query, userId) match {
    case Success(sql)   => prepareQueryExport(sql, targetFormat).map { formatExport(_, targetFormat) }
    case Failure(error) => Future.failed { error }
  }

  private def doQuickExec(params: DatasetParams, query: Query, targetFormat: FileDataFormat, userId: String) = for {
    tableName  <- extractTableName(params, userId)
    jdbcResult <- queryService.exec(query, tableName, userId)
    data       <- transform(jdbcResult, targetFormat)
  } yield data

  // API

  protected def quickExec(params: DatasetParams, query: Query, targetFormat: FileDataFormat, userId: String) = analyzeQuery(params, query, userId) match {
    case Success(analysis) if analysis.memoryEstimation <= impalaConfig.memoryEstimationLimit => doQuickExec(params, query, targetFormat, userId).~>[Future].map { respond(params, _, targetFormat) }
    case Success(_)                                                                           => failQuickExec(params, targetFormat)
    case Failure(error)                                                                       => Future.failed { error }
  }

  protected def batchExec(params: DatasetParams, query: Query, targetFormat: FileDataFormat, userId: String) =
    doBatchExec(params, query, targetFormat, userId).map { respond(params, _, targetFormat) }

  protected def exec(params: DatasetParams, query: Query, userId: String, targetFormat: FileDataFormat, method: DownloadMethod) = method match {
    case QuickDownloadMethod => quickExec(params, query, targetFormat, userId)
    case BatchDownloadMethod => batchExec(params, query, targetFormat, userId)
  }

}