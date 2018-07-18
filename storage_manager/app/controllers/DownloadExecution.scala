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

import cats.syntax.show.toShow
import daf.dataset._
import daf.filesystem.{ FileDataFormat, fileFormatShow }
import daf.instances.FileSystemInstance
import it.gov.daf.common.utils._
import it.gov.daf.common.sso.UserGroupInformationSyntax

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait DownloadExecution { this: DatasetController with DatasetExport with FileSystemInstance =>

  // Failures

  private def failQuickDownload(params: DatasetParams, targetFormat: FileDataFormat) = Future.successful {
    TemporaryRedirect {
      s"${controllers.routes.DatasetController.getDataset(params.catalogUri, targetFormat.show, "batch").url}"
    }
  }

  // Retrievals

  private def retrieveFileInfo(path: String, userId: String) = (proxyUser as userId) { downloadService.fileInfo(path) }

  private def retrieveTableInfo(tableName: String, userId: String) = (proxyUser as userId) { downloadService.tableInfo(tableName) }

  // Executions

  private def doTableExport(params: KuduDatasetParams, userId: String, targetFormat: FileDataFormat) = retrieveTableInfo(params.table, userId) match {
    case Success(_)     => prepareTableExport(params.table, targetFormat, params.extraParams).map { formatExport(_, targetFormat) }
    case Failure(error) => Future.failed { error }
  }

  private def doFileExport(params: FileDatasetParams, userId: String, targetFormat: FileDataFormat) = retrieveFileInfo(params.path, userId) match {
    case Success(pathInfo) => prepareFileExport(pathInfo, params.format, targetFormat, params.extraParams).map { formatExport(_, targetFormat) }
    case Failure(error)    => Future.failed { error }
  }

  private def doQuickFile(params: DatasetParams, targetFormat: FileDataFormat) = prepareDirect(params, targetFormat).map { respond(_, params.name, targetFormat) }.~>[Future]

  private def quickFileDownload(params: FileDatasetParams, userId: String, targetFormat: FileDataFormat) = retrieveFileInfo(params.path, userId) match {
    case Success(pathInfo) if pathInfo.estimatedSize <= exportConfig.sizeThreshold => doQuickFile(params, targetFormat)
    case Success(pathInfo)                                                         => failQuickDownload(params, targetFormat)
    case Failure(error)                                                            => Future.failed { error }
  }

  // API

  protected def quickDownload(params: DatasetParams, userId: String, targetFormat: FileDataFormat) = params match {
    case fileParams: FileDatasetParams => quickFileDownload(fileParams, userId, targetFormat)
    case kuduParams: KuduDatasetParams => failQuickDownload(kuduParams, targetFormat) // no quick download option for kudu
  }

  protected def batchDownload(params: DatasetParams, userId: String, targetFormat: FileDataFormat) = params match {
    case kuduParams: KuduDatasetParams => doTableExport(kuduParams, userId, targetFormat).map { respond(_, kuduParams.table, targetFormat) }
    case fileParams: FileDatasetParams => doFileExport(fileParams, userId, targetFormat).map { respond(_, fileParams.name, targetFormat) }
  }

  protected def download(params: DatasetParams, userId: String, targetFormat: FileDataFormat, method: DownloadMethod) = method match {
    case QuickDownloadMethod => quickDownload(params, userId, targetFormat)
    case BatchDownloadMethod => batchDownload(params, userId, targetFormat)
  }

}
