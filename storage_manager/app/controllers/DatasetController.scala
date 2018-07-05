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

import akka.actor.ActorSystem
import com.google.inject.{ Inject, Singleton }
import api.DatasetControllerAPI
import config.{ FileExportConfig, ImpalaConfig }
import daf.catalogmanager.CatalogManagerClient
import daf.dataset.export.FileExportService
import daf.dataset._
import daf.dataset.query.jdbc.JdbcQueryService
import daf.dataset.query.Query
import daf.dataset.query.json.QueryFormats.reader
import daf.instances.{ FileSystemInstance, ImpalaTransactorInstance }
import daf.web._
import daf.filesystem.{ DownloadableFormats, FileDataFormat }
import it.gov.daf.common.config.{ ConfigReadException, Read }
import org.apache.hadoop.conf.{ Configuration => HadoopConfiguration }
import org.apache.hadoop.fs.FileSystem
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class DatasetController @Inject()(configuration: Configuration,
                                  playSessionStore: PlaySessionStore,
                                  protected val ws: WSClient,
                                  protected implicit val actorSystem: ActorSystem,
                                  protected implicit val ec: ExecutionContext)
  extends AbstractController(configuration, playSessionStore)
  with DatasetControllerAPI
  with QueryExecution
  with BulkDownload
  with FileSystemInstance {

  implicit val fileSystem = FileSystem.get(new HadoopConfiguration)

  private val queryJson = BodyParsers.parse.json[Query]

  private val kuduMaster = Read.string { "kudu.master" }.!.read(configuration) match {
    case Success(result) => result
    case Failure(error)  => throw ConfigReadException(s"Unable to configure [dataset-manager]", error)
  }

  protected val exportConfig = FileExportConfig.reader.read(configuration) match {
    case Success(result) => result
    case Failure(error)  => throw ConfigReadException(s"Unable to configure [dataset-manager]", error)
  }

  protected val impalaConfig = ImpalaConfig.reader.read(this.configuration) match {
    case Success(config) => config
    case Failure(error)  => throw ConfigReadException(s"Unable to configure [impala-jdbc]", error)
  }

  protected val datasetService    = new DatasetService(configuration.underlying)
  protected val queryService      = new JdbcQueryService(impalaConfig) with ImpalaTransactorInstance
  protected val downloadService   = new DownloadService(kuduMaster)
  protected val fileExportService = new FileExportService(exportConfig, kuduMaster)

  protected val catalogClient = CatalogManagerClient.fromConfig(configuration)

  private def retrieveCatalog(auth: String, uri: String) = for {
    catalog <- catalogClient.getById(auth, uri)
    params  <- DatasetParams.fromCatalog(catalog)
  } yield params

  private def retrieveBulkData(uri: String, auth: String, userId: String, targetFormat: FileDataFormat) = retrieveCatalog(auth, uri) match {
    case Success(params) => bulkDownload(params, userId, targetFormat)
    case Failure(error)  => Future.failed { error }
  }

  private def executeQuery(query: Query, uri: String, auth: String, userId: String, targetFormat: FileDataFormat) = retrieveCatalog(auth, uri).flatMap {
    exec(_, query, targetFormat, userId)
  }

  // API

  def getSchema(uri: String): Action[AnyContent] = Actions.hadoop(proxyUser).securedAttempt { (_, auth, _) =>
    for {
      params <- retrieveCatalog(auth, uri)
      schema <- datasetService.schema(params)
    } yield Ok { schema.prettyJson } as JSON
  }

  def getDataset(uri: String, format: String = "csv"): Action[AnyContent] = Actions.basic.securedAsync { (_, auth, userId) =>
    format.toLowerCase match {
      case DownloadableFormats(targetFormat) => retrieveBulkData(uri, auth, userId, targetFormat)
      case _                                 => Future.successful { Results.BadRequest(s"Invalid download format [$format], must be one of [csv | json]") }
    }
  }

  def queryDataset(uri: String, format: String = "csv"): Action[Query] = Actions.basic.securedAttempt(queryJson) { (request, auth, userId) =>
    format.toLowerCase match {
      case DownloadableFormats(targetFormat) => executeQuery(request.body, uri, auth, userId, targetFormat)
      case _                                 => Success { Results.BadRequest(s"Invalid download format [$format], must be one of [csv | json]") }
    }
  }

}
