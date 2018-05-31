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

package daf.dataset.export

import akka.actor.ActorRefFactory
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import config.FileExportConfig
import daf.filesystem._
import org.apache.hadoop.fs.{ FileSystem, Path }
import org.apache.livy.client.http.HttpClientFactory

import scala.concurrent.Future
import scala.util.{ Failure, Success }

class FileExportService(fileExportConfig: FileExportConfig)(implicit actorRefFactory: ActorRefFactory, fileSystem: FileSystem) {

  private implicit val askTimeout = Timeout.durationToTimeout { fileExportConfig.exportTimeout }

  private val exportRouter = actorRefFactory.actorOf {
    RoundRobinPool(fileExportConfig.numSessions).props {
      FileExportActor.props(new HttpClientFactory, fileExportConfig)
    }
  }

  implicit val executionContext = actorRefFactory.dispatcher

  def export(path: Path, fromFormat: FileDataFormat, toFormat: FileDataFormat): Future[String] =
    { exportRouter ? ExportFile(path.asUriString, fromFormat, toFormat) }.flatMap {
      case Success(dataPath: String) => Future.successful { dataPath }
      case Success(invalidValue)     => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a string but got: [$invalidValue]") }
      case Failure(error)            => Future.failed { error }
      case unexpectedValue           => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a Try[String], but received [$unexpectedValue]") }
    }

  def export(info: PathInfo, toFormat: FileDataFormat): Future[String] = info match {
    case dirInfo: DirectoryInfo if dirInfo.hasMixedFormats      => Future.failed {
      new IllegalArgumentException(s"Unable to prepare export: directory [${dirInfo.path.getName}] has mixed formats")
    }
    case dirInfo: DirectoryInfo if dirInfo.hasMixedCompressions => Future.failed {
      new IllegalArgumentException(s"Unable to prepare export: directory [${dirInfo.path.getName}] has mixed compressions")
    }
    case dirInfo: DirectoryInfo if dirInfo.isEmpty              => Future.failed {
      new IllegalArgumentException(s"Unable to prepare export: directory [${dirInfo.path.getName}] is empty")
    }
    case dirInfo: DirectoryInfo                                 => export(dirInfo.path, dirInfo.fileFormats.head, toFormat) // .head is guarded by .isEmpty
    case fileInfo: FileInfo                                     => export(fileInfo.path, fileInfo.format, toFormat)
  }

}