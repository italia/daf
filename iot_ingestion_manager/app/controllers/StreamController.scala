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

import api.StreamAPI
import client.CatalogClient
import com.google.inject.Inject
import config.StreamApplicationConfig
import daf.stream.StreamService
import it.gov.daf.common.web.{ Actions, SecureController }
import it.gov.daf.common.utils._
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.BodyParsers
import representation.StreamData
import representation.json.StreamDataReads

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class StreamController @Inject()(configuration: Configuration,
                                 playSessionStore: PlaySessionStore,
                                 wsClient: WSClient,
                                 protected implicit val ec: ExecutionContext) extends SecureController(configuration, playSessionStore) with StreamAPI {

  private val streamDataJson = BodyParsers.parse.json[StreamData] { StreamDataReads.streamData }

  private val applicationConfig = StreamApplicationConfig.reader.read { configuration } match {
    case Success(config) => config
    case Failure(error)  => throw new RuntimeException("Unable to configure [iot-manager]", error)
  }

  private val streamService = new StreamService(applicationConfig.kafkaConfig)
  private val catalogClient = new CatalogClient(wsClient, applicationConfig.catalogUrl)

  private def createStream(streamData: StreamData) = streamService.createStream(streamData).map {
    case query if query.isActive => Created
    case _                       => InternalServerError { s"Query with id [${streamData.id}] failed to start" }
  }

  def createRaw = Actions.basic.attempt(streamDataJson) { request =>
    createStream { request.body }
  }

  def create(catalogId: String) = Actions.basic.securedAsync { (_, auth, _) =>
    for {
      catalog    <- catalogClient.getCatalog(catalogId, auth)
      streamData <- StreamData.fromCatalog(catalog).~>[Future]
      result     <- createStream(streamData).~>[Future]
    } yield result
  }

  def update(catalogId: String) = Actions.basic { _ => NotImplemented }

}
