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

import akka.stream.scaladsl.StreamConverters
import cats.syntax.show.toShow
import controllers.DatasetController
import daf.filesystem.{ CsvFileFormat, FileDataFormat, JsonFileFormat, PathInfo, fileFormatShow }
import daf.web._
import daf.instances.FileSystemInstance

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait BulkDownload { this: DatasetController with FileSystemInstance =>

  private def prepareDirect(params: DatasetParams, targetFormat: FileDataFormat) = targetFormat match {
    case JsonFileFormat => datasetService.jsonData(params)
    case CsvFileFormat  => datasetService.csvData(params)
    case _              => Failure { new IllegalArgumentException("Unable to prepare download; only CSV and JSON are permitted") }
  }

  private def prepareFileExport(pathInfo: PathInfo, sourceFormat: FileDataFormat, targetFormat: FileDataFormat, extraParams: ExtraParams) =
    fileExportService.exportFile(pathInfo.path, sourceFormat, targetFormat, extraParams).map { downloadService.openPath }.flatMap {
      case Success(stream) => Future.successful {
        StreamConverters.fromInputStream { () => stream }
      }
      case Failure(error)  => Future.failed { error }
    }

  private def prepareTableExport(table: String, targetFormat: FileDataFormat, extraParams: ExtraParams) =
    fileExportService.exportTable(table, targetFormat, extraParams).map { downloadService.openPath }.flatMap {
      case Success(stream) => Future.successful {
        StreamConverters.fromInputStream { () => stream }
      }
      case Failure(error)  => Future.failed { error }
    }

  // Web
  // Successes

  private def directDownload(params: DatasetParams, targetFormat: FileDataFormat) = prepareDirect(params, targetFormat) match {
    case Success(data)  => Future.successful {
      Ok.chunked(data).withHeaders(
        CONTENT_DISPOSITION -> s"""attachment; filename="${params.name}.${targetFormat.show}"""",
        CONTENT_TYPE        -> contentType(targetFormat)
      )
    }
    case Failure(error) => Future.failed { error }
  }

  private def fileExportDownload(pathInfo: PathInfo, sourceFormat: FileDataFormat, targetFormat: FileDataFormat, extraParams: ExtraParams) =
    prepareFileExport(pathInfo, sourceFormat, targetFormat, extraParams).map { formatExport(_, targetFormat) }.map {
      Ok.chunked(_).withHeaders(
        CONTENT_DISPOSITION -> s"""attachment; filename="${pathInfo.path.getName}.${targetFormat.show}"""",
        CONTENT_TYPE        -> contentType(targetFormat)
      )
    }

  private def tableExportDownload(table: String, targetFormat: FileDataFormat, extraParams: ExtraParams) =
    prepareTableExport(table, targetFormat, extraParams).map { formatExport(_, targetFormat) }.map {
      Ok.chunked(_).withHeaders(
        CONTENT_DISPOSITION -> s"""attachment; filename="$table.${targetFormat.show}"""",
        CONTENT_TYPE        -> contentType(targetFormat)
      )
    }

  // Failures

  private def failQuickDownload(params: DatasetParams, targetFormat: FileDataFormat) = Future.successful {
    TemporaryRedirect {
      s"${controllers.routes.DatasetController.getDataset(params.catalogUri, targetFormat.show).url}?format=${targetFormat.show}&method=batch"
    }
  }

  // Retrievals

  private def retrieveFileInfo(path: String, userId: String) = (proxyUser as userId) { downloadService.fileInfo(path) }

  private def retrieveTableInfo(tableName: String, userId: String) = (proxyUser as userId) { downloadService.tableInfo(tableName) }

  private def tableDownload(params: KuduDatasetParams, userId: String, targetFormat: FileDataFormat) = retrieveTableInfo(params.table, userId) match {
    case Success(_)     => tableExportDownload(params.table, targetFormat, params.extraParams)
    case Failure(error) => Future.failed { error }
  }

  private def bulkFileDownload(params: FileDatasetParams, userId: String, targetFormat: FileDataFormat) = retrieveFileInfo(params.path, userId) match {
    case Success(pathInfo) => fileExportDownload(pathInfo, params.format, targetFormat, params.extraParams)
    case Failure(error)    => Future.failed { error }
  }

  private def quickFileDownload(params: FileDatasetParams, userId: String, targetFormat: FileDataFormat) = retrieveFileInfo(params.path, userId) match {
    case Success(pathInfo) if pathInfo.estimatedSize <= exportConfig.sizeThreshold => directDownload(params, targetFormat)
    case Success(pathInfo)                                                         => failQuickDownload(params, targetFormat)
    case Failure(error)                                                            => Future.failed { error }
  }

  // API

  protected def quickDownload(params: DatasetParams, userId: String, targetFormat: FileDataFormat) = params match {
    case fileParams: FileDatasetParams => quickFileDownload(fileParams, userId, targetFormat)
    case kuduParams: KuduDatasetParams => failQuickDownload(kuduParams, targetFormat) // no quick download option for kudu
  }

  protected def batchDownload(params: DatasetParams, userId: String, targetFormat: FileDataFormat) = params match {
    case kuduParams: KuduDatasetParams => tableDownload(kuduParams, userId, targetFormat)
    case fileParams: FileDatasetParams => bulkFileDownload(fileParams, userId, targetFormat)
  }

  protected def download(params: DatasetParams, userId: String, targetFormat: FileDataFormat, method: DownloadMethod) = method match {
    case QuickDownloadMethod => quickDownload(params, userId, targetFormat)
    case BatchDownloadMethod => batchDownload(params, userId, targetFormat)
  }

}
