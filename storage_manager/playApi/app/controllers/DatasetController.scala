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
import daf.dataset.{ BulkDownload, Query }
import daf.dataset.json._
import daf.instance.FileSystemInstance
import io.swagger.annotations._
import it.gov.daf.common.config.ConfigReadException
import it.teamdigitale.filesystem.{ FileDataFormat, FileDataFormats, JsonFileFormat, PathInfo, RawFileFormat }
import it.teamdigitale.web._
import org.apache.hadoop.conf.{ Configuration => HadoopConfiguration }
import org.apache.hadoop.fs.FileSystem
import org.pac4j.play.store.PlaySessionStore
import play.api.{ Configuration, Logger }
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class DatasetController @Inject()(configuration: Configuration,
                                  playSessionStore: PlaySessionStore,
                                  protected val ws: WSClient,
                                  protected val parsers: BodyParsers,
                                  protected implicit val actorSystem: ActorSystem,
                                  protected implicit val ec: ExecutionContext)
  extends AbstractController(configuration, playSessionStore)
  with DatasetControllerAPI
  with BulkDownload
  with FileSystemInstance {

  private val log = Logger(this.getClass)

  implicit val fileSystem = FileSystem.get(new HadoopConfiguration)

  private val queryJson = parsers.parse.json[Query]

  def getSchema(uri: String): Action[AnyContent] = Actions.hadoop(proxyUser).secured { (request, auth, _) =>
    log.info(s"processing request=${request.method} for schema")

    datasetService.schema(auth, uri) match {
      case Success(st) =>
        log.info(s"response request=${request.method} with value=$st")
        Ok(st.prettyJson)
      case Failure(ex) =>
        log.error(s"processing request=${request.method} with uri=$uri error=${ex.getMessage}", ex)
        BadRequest(ex.getMessage).as(JSON)
    }
  }

  def getDataset(uri: String, format: String = "csv"): Action[AnyContent] = Actions.basic.securedAsync { (request, auth, userId) =>
    log.info(s"processing request=${request.method} with uri=$uri")

    format.toLowerCase match {
      case FileDataFormats(targetFormat) => bulkDownload(uri, auth, userId, targetFormat)
      case _                             => Future.successful { Results.BadRequest(s"Invalid download format [$format], must be one of [csv | json]") }
    }
  }

  def queryDataset(uri: String): Action[Query] = Actions.hadoop(proxyUser).secured(queryJson) { (request, auth, _) =>
    log.info(s"processing request=${request.method} with uri=$uri")

    datasetService.query(auth, uri, request.body) match {
      case Success(df) =>
        val records = s"[${df.toJSON.collect().mkString(",")}]"
        log.info(s"response request=${request.method} with records=$records")
        df.unpersist()
        Ok(records)
      case Failure(ex) =>
        log.error(s"processing request=${request.method} with uri=$uri error=${ex.getMessage}", ex)
        BadRequest(ex.getMessage).as(JSON)
    }
  }

}
