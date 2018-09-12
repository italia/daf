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
import daf.dataset.ExtraParams
import daf.dataset.export.cleanup.FileExportCleanupActor
import daf.filesystem._
import org.apache.hadoop.fs.{ FileSystem, Path }
import org.apache.livy.client.http.HttpClientFactory

import scala.concurrent.Future
import scala.util.{ Failure, Success }

/**
  * Facade that allows for creating file and table export jobs, launching them against a Livy server. The number of Livy
  * sessions is controlled via the a router pool of Akka actors, each having its own session.
  * @param fileExportConfig the configuration settings for the export
  * @param kuduMaster the connection string for the Kudu cluster master
  * @param actorRefFactory the `ActorRefFactory` used to generate the router pool and its actors
  * @param fileSystem the `FileSystem` instance used for direct HDFS access
  */
class FileExportService(fileExportConfig: FileExportConfig, kuduMaster: String, defaultLimit: Option[Int] = None)(implicit actorRefFactory: ActorRefFactory, fileSystem: FileSystem) {

  private implicit val askTimeout = Timeout.durationToTimeout { fileExportConfig.exportTimeout }

  private val exportRouter = actorRefFactory.actorOf {
    RoundRobinPool(fileExportConfig.numSessions).props {
      FileExportActor.props(new HttpClientFactory, kuduMaster, fileExportConfig, defaultLimit)
    }
  }

  private val exportCleaner = actorRefFactory.actorOf { FileExportCleanupActor.props(fileExportConfig) }

  implicit val executionContext = actorRefFactory.dispatcher

  /**
    * Exports a `table` from Kudu to the specified file format.
    * @param table the name of table to export
    * @param toFormat the [[daf.filesystem.FileDataFormat]] of the expected output file
    * @param extraParams a `Map[String, String]` of additional parameters to be passed to the export job
    * @param limit an optional integer that is used to limit the number of returned rows
    * @return `Future` containing the path to the exported file
    */
  def exportTable(table: String, toFormat: FileDataFormat, extraParams: ExtraParams = Map.empty[String, String], limit: Option[Int] = None): Future[String] =
    { exportRouter ? ExportTable(table, toFormat, extraParams, limit) }.flatMap {
      case Success(dataPath: String) => Future.successful { dataPath }
      case Success(invalidValue)     => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a string but got: [$invalidValue]") }
      case Failure(error)            => Future.failed { error }
      case unexpectedValue           => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a Try[String], but received [$unexpectedValue]") }
    }

  /**
    * Export the result of running an SQL query to a file, written in the given format.
    * @param query the SQL to run
    * @param toFormat the [[daf.filesystem.FileDataFormat]] of the expected output file
    * @param extraParams a `Map[String, String]` of additional parameters to be passed to the export job
    * @return a `Future` containing the parth to the exported file
    */
  def exportQuery(query: String, toFormat: FileDataFormat, extraParams: ExtraParams = Map.empty[String, String]): Future[String] =
    { exportRouter ? ExportQuery(query, toFormat, extraParams) }.flatMap {
      case Success(dataPath: String) => Future.successful { dataPath }
      case Success(invalidValue)     => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a string but got: [$invalidValue]") }
      case Failure(error)            => Future.failed { error }
      case unexpectedValue           => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a Try[String], but received [$unexpectedValue]") }
    }

  /**
    * Exports a `file` from a format to another.
    * @param path the path of the file to export
    * @param fromFormat the [[daf.filesystem.FileDataFormat]] of the input file
    * @param toFormat the [[daf.filesystem.FileDataFormat]] of the expected output file
    * @param extraParams a `Map[String, String]` of additional parameters to be passed to the export job
    * @param limit an optional integer that is used to limit the number of returned rows
    * @return `Future` containing the path to the exported file
    */
  def exportFile(path: Path, fromFormat: FileDataFormat, toFormat: FileDataFormat, extraParams: ExtraParams = Map.empty[String, String], limit: Option[Int] = None): Future[String] =
    { exportRouter ? ExportFile(path.asUriString, fromFormat, toFormat, extraParams, limit) }.flatMap {
      case Success(dataPath: String) => Future.successful { dataPath }
      case Success(invalidValue)     => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a string but got: [$invalidValue]") }
      case Failure(error)            => Future.failed { error }
      case unexpectedValue           => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a Try[String], but received [$unexpectedValue]") }
    }

  /**
    * Exports a `file` from a format to another.
    * @param info the [[daf.filesystem.PathInfo]] instance of input data, which can be a directory or a file
    * @param toFormat the [[daf.filesystem.FileDataFormat]] of the expected output file
    * @param extraParams a `Map[String, String]` of additional parameters to be passed to the export job
    * @param limit an optional integer that is used to limit the number of returned rows
    * @return `Future` containing the path to the exported file
    */
  def exportPath(info: PathInfo, toFormat: FileDataFormat, extraParams: ExtraParams = Map.empty[String, String], limit: Option[Int] = None): Future[String] = info match {
    case dirInfo: DirectoryInfo if dirInfo.hasMixedFormats      => Future.failed {
      new IllegalArgumentException(s"Unable to prepare export: directory [${dirInfo.path.getName}] has mixed formats")
    }
    case dirInfo: DirectoryInfo if dirInfo.hasMixedCompressions => Future.failed {
      new IllegalArgumentException(s"Unable to prepare export: directory [${dirInfo.path.getName}] has mixed compressions")
    }
    case dirInfo: DirectoryInfo if dirInfo.isEmpty              => Future.failed {
      new IllegalArgumentException(s"Unable to prepare export: directory [${dirInfo.path.getName}] is empty")
    }
    case dirInfo: DirectoryInfo                                 => exportFile(dirInfo.path, dirInfo.fileFormats.head, toFormat, extraParams, limit) // .head is guarded by .isEmpty
    case fileInfo: FileInfo                                     => exportFile(fileInfo.path, fileInfo.format, toFormat, extraParams, limit)
  }

}