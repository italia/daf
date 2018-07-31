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
import com.google.inject.Inject
import api.DatasetControllerAPI
import cats.MonadError
import cats.instances.future.catsStdInstancesForFuture
import config.{ FileExportConfig, ImpalaConfig }
import daf.catalogmanager.CatalogManagerClient
import daf.dataset.export.FileExportService
import daf.dataset._
import daf.dataset.query.jdbc.JdbcQueryService
import daf.dataset.query.Query
import daf.dataset.query.json.QueryFormats.reader
import daf.error.InvalidRequestException
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
import scala.language.higherKinds
import scala.util.{ Failure, Success }

class DatasetController @Inject()(configuration: Configuration,
                                  playSessionStore: PlaySessionStore,
                                  protected val ws: WSClient,
                                  protected implicit val actorSystem: ActorSystem,
                                  protected implicit val ec: ExecutionContext)
  extends AbstractController(configuration, playSessionStore)
  with DatasetControllerAPI
  with QueryExecution
  with DownloadExecution
  with DatasetExport
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

  private def defaultLimit = Read.int { "daf.row_limit" }.read(configuration) getOrElse None

  protected val datasetService    = new DatasetService(configuration.underlying)
  protected val queryService      = new JdbcQueryService(impalaConfig, defaultLimit) with ImpalaTransactorInstance
  protected val downloadService   = new DownloadService(kuduMaster)
  protected val fileExportService = new FileExportService(exportConfig, kuduMaster)

  protected val catalogClient = CatalogManagerClient.fromConfig(configuration)

  private def retrieveCatalog(auth: String, uri: String) = for {
    catalog <- catalogClient.getById(auth, uri)
    params  <- DatasetParams.fromCatalog(catalog)
  } yield params

  private def retrieveBulkData(uri: String, auth: String, userId: String, targetFormat: FileDataFormat, method: DownloadMethod, limit: Option[Int]) = retrieveCatalog(auth, uri) match {
    case Success(params) => download(params, userId, targetFormat, method, limit)
    case Failure(error)  => Future.failed { error }
  }

  private def executeQuery(query: Query, uri: String, auth: String, userId: String, targetFormat: FileDataFormat, method: DownloadMethod) = retrieveCatalog(auth, uri) match {
    case Success(params) => exec(params, query, userId, targetFormat, method)
    case Failure(error)  => Future.failed { error }
  }

  private def checkTargetFormat[M[_]](format: String)(implicit M: MonadError[M, Throwable]): M[FileDataFormat] = format.toLowerCase match {
    case DownloadableFormats(targetFormat) => M.pure { targetFormat }
    case _                                 => M.raiseError { InvalidRequestException(s"Invalid download format [$format], must be one of [csv | json]") }
  }

  private def checkDownloadMethod[M[_]](method: String)(implicit M: MonadError[M, Throwable]): M[DownloadMethod] = method.toLowerCase match {
    case DownloadMethods(downloadMethod) => M.pure { downloadMethod }
    case _                               => M.raiseError { InvalidRequestException(s"Invalid download method [$method], must be one of [quick | batch]") }
  }

  // API

  def getSchema(uri: String): Action[AnyContent] = Actions.hadoop(proxyUser).securedAttempt { (_, auth, _) =>
    for {
      params <- retrieveCatalog(auth, uri)
      schema <- datasetService.schema(params)
    } yield Ok { schema.prettyJson } as JSON
  }

  def getDataset(uri: String, format: String = "json", method: String = "quick", limit: Option[Int] = None): Action[AnyContent] = Actions.basic.securedAsync { (_, auth, userId) =>
    for {
      targetFormat   <- checkTargetFormat[Future](format)
      downloadMethod <- checkDownloadMethod[Future](method)
      result         <- retrieveBulkData(uri, auth, userId, targetFormat, downloadMethod, limit)
    } yield result
  }

  def queryDataset(uri: String, format: String = "json", method: String = "quick"): Action[Query] = Actions.basic.securedAsync(queryJson) { (request, auth, userId) =>
    for {
      targetFormat   <- checkTargetFormat[Future](format)
      downloadMethod <- checkDownloadMethod[Future](method)
      result         <- executeQuery(request.body, uri, auth, userId, targetFormat, downloadMethod)
    } yield result
  }

}
