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

import akka.stream.scaladsl.{ Source, StreamConverters }
import cats.syntax.show.toShow
import daf.dataset.{ DatasetParams, ExtraParams }
import daf.filesystem.{ CsvFileFormat, FileDataFormat, JsonFileFormat, PathInfo, fileFormatShow }
import daf.web.contentType

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait DatasetExport { this: DatasetController =>

  protected def prepareDirect(params: DatasetParams, targetFormat: FileDataFormat, limit: Option[Int]) = targetFormat match {
    case JsonFileFormat => datasetService.jsonData(params, limit)
    case CsvFileFormat  => datasetService.csvData(params, limit)
    case _              => Failure { new IllegalArgumentException("Unable to prepare download; only CSV and JSON are permitted") }
  }

  protected def prepareFileExport(pathInfo: PathInfo, sourceFormat: FileDataFormat, targetFormat: FileDataFormat, extraParams: ExtraParams) =
    fileExportService.exportFile(pathInfo.path, sourceFormat, targetFormat, extraParams).map { downloadService.openPath }.flatMap {
      case Success(stream) => Future.successful {
        StreamConverters.fromInputStream { () => stream }
      }
      case Failure(error)  => Future.failed { error }
    }

  protected def prepareTableExport(table: String, targetFormat: FileDataFormat, extraParams: ExtraParams) =
    fileExportService.exportTable(table, targetFormat, extraParams).map { downloadService.openPath }.flatMap {
      case Success(stream) => Future.successful {
        StreamConverters.fromInputStream { () => stream }
      }
      case Failure(error)  => Future.failed { error }
    }

  protected def prepareQueryExport(query: String, targetFormat: FileDataFormat) =
    fileExportService.exportQuery(query, targetFormat).map { downloadService.openPath }.flatMap {
      case Success(stream) => Future.successful {
        StreamConverters.fromInputStream { () => stream }
      }
      case Failure(error)  => Future.failed { error }
    }

  protected def respond(data: Source[String, _], fileName: String, targetFormat: FileDataFormat) = Ok.chunked(data).withHeaders(
    CONTENT_DISPOSITION -> s"""attachment; filename="$fileName.${targetFormat.show}"""",
    CONTENT_TYPE        -> contentType(targetFormat)
  )

}
