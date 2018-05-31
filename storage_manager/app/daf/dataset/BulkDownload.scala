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
import config.FileExportConfig
import controllers.DatasetController
import daf.dataset.export.FileExportService
import daf.filesystem.{ FileDataFormat, JsonFileFormat, RawFileFormat }
import daf.web._
import daf.instances.FileSystemInstance
import it.gov.daf.common.config.ConfigReadException

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait BulkDownload { this: DatasetController with FileSystemInstance =>

  private val exportConfig = FileExportConfig.reader.read(configuration) match {
    case Success(result) => result
    case Failure(error)  => throw ConfigReadException(s"Unable to configure [dataset-manager]", error)
  }

  protected lazy val datasetService    = new DatasetService(configuration.underlying, ws)(ec)
  protected lazy val downloadService   = new DownloadService
  protected lazy val fileExportService = new FileExportService(exportConfig)

  private def prepareDirect(path: String, auth: String, targetFormat: FileDataFormat) = targetFormat match {
    case JsonFileFormat => datasetService.jsonData(auth, path)
    case RawFileFormat  => datasetService.csvData(auth, path)
    case _              => Failure { new IllegalArgumentException("Unable to prepare download; only CSV and JSON are permitted") }
  }

  private def prepareExport(path: String, targetFormat: FileDataFormat) = downloadService.info(path) match {
    case Success(dataInfo) => fileExportService.export(dataInfo, targetFormat)
    case Failure(error)    => Future.failed { error }
  }

  private def directDownload(path: String, auth: String, targetFormat: FileDataFormat) = prepareDirect(path, auth, targetFormat) match {
    case Success(data)  => Future.successful { Ok.chunked(data) }
    case Failure(error) => Future.failed { error }
  }

  private def exportDownload(path: String, targetFormat: FileDataFormat) = prepareExport(path, targetFormat).map { downloadService.open }.flatMap {
    case Failure(error)  => Future.failed { error }
    case Success(stream) => Future.successful {
      Ok.chunked {
        StreamConverters.fromInputStream { () => stream }
      }
    }
  }

  private def retrieveFileInfo(path: String, userId: String) = (proxyUser as userId) { downloadService.info(path) }

  protected def bulkDownload(path: String, auth: String, userId: String, targetFormat: FileDataFormat) = retrieveFileInfo(path, userId) match {
    case Success(pathInfo) if pathInfo.estimatedSize <= exportConfig.sizeThreshold => directDownload(path, auth, targetFormat)
    case Success(_)                                                                => exportDownload(path, targetFormat)
    case Failure(error)                                                            => Future.failed { error }
  }


}
