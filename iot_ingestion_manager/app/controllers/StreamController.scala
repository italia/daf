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
import api.StreamAPI
import client.CatalogClient
import com.google.inject.Inject
import config.StreamApplicationConfig
import daf.stream.{ AvroPayloadValidator, NoPayloadValidator, ProducerService, SparkConsumerService }
import it.gov.daf.common.web.Actions
import it.gov.daf.common.utils._
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import play.api.mvc.BodyParsers
import representation.{ Event, StreamData }
import representation.json.{ EventReads, StreamDataReads }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class StreamController @Inject()(configuration: Configuration,
                                 playSessionStore: PlaySessionStore,
                                 wsClient: WSClient,
                                 cacheApi: CacheApi,
                                 protected implicit val actorSystem: ActorSystem,
                                 protected implicit val ec: ExecutionContext) extends StreamAPI(configuration, playSessionStore) {

  private val streamDataJson = BodyParsers.parse.json[StreamData] { StreamDataReads.streamData }
  private val eventJson  = BodyParsers.parse.json[Event] { EventReads.event }

  private val applicationConfig = StreamApplicationConfig.reader.read { configuration } match {
    case Success(config) => config
    case Failure(error)  => throw new RuntimeException("Unable to configure [iot-manager]", error)
  }

  private val payloadValidator = applicationConfig.validator match {
    case "none" | "off" => NoPayloadValidator
    case "avro"         => AvroPayloadValidator
    case other          => throw new RuntimeException(s"Unable to configure [iot-manager]: unsupported payload validator [$other]")
  }

  private val consumerService = new SparkConsumerService(applicationConfig.kafkaConfig)
  private val producerService = new ProducerService(applicationConfig.kafkaConfig, payloadValidator)
  private val catalogClient   = new CatalogClient(wsClient, cacheApi, applicationConfig.catalogUrl)

  def register = Actions.basic.attempt(streamDataJson) { request =>
    consumerService.createConsumer(request.body).map { _ => Created }
  }

  def registerCatalog(catalogId: String) = Actions.basic.securedAsync { (_, auth, _) =>
    for {
      catalog    <- catalogClient.getCatalog(catalogId, auth)
      streamData <- StreamData.fromCatalog(catalog).~>[Future]
      _          <- consumerService.createConsumer(streamData).~>[Future]
    } yield Created
  }

  def update(catalogId: String) = Actions.basic.securedAsync(eventJson) { (request, auth, userId) =>
    for {
      catalog    <- catalogClient.getCatalog(catalogId, auth)
      streamData <- StreamData.fromCatalog(catalog).~>[Future]
      _          <- producerService.sendMessage(streamData, userId, request.body)
    } yield Ok
  }

}
