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
import client.{ CatalogClient, KyloClient }
import com.google.inject.Inject
import config.{ KyloConfig, StreamApplicationConfig }
import daf.stream._
import daf.stream.kylo.KyloConsumerService
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
                                 wsClient: WSClient,
                                 cacheApi: CacheApi,
                                 playSessionStore: PlaySessionStore,
                                 protected implicit val actorSystem: ActorSystem,
                                 protected implicit val ec: ExecutionContext) extends StreamAPI(configuration, playSessionStore) {

  private val streamDataJson = BodyParsers.parse.json[StreamData] { StreamDataReads.streamData }
  private val eventJson      = BodyParsers.parse.json[Event] { EventReads.event }

  private val applicationConfig = StreamApplicationConfig.reader.read { configuration } match {
    case Success(config) => config
    case Failure(error)  => throw new RuntimeException("Unable to configure [iot-manager]", error)
  }

  private val payloadValidator = applicationConfig.validator match {
    case "none" | "off" => NoPayloadValidator
    case "avro"         => AvroPayloadValidator
    case "avro-json"    => AvroJsonPayloadValidator
    case other          => throw new RuntimeException(s"Unable to configure [iot-manager]: unsupported payload validator [$other]")
  }

  private val catalogClient   = new CatalogClient(wsClient, cacheApi, applicationConfig.catalogUrl)
  private val kyloClient      = new KyloClient(wsClient, cacheApi, applicationConfig.kyloConfig)
  private val streamService   = new StreamService(cacheApi, catalogClient)
  private val consumerService = new KyloConsumerService(applicationConfig.kafkaConfig, applicationConfig.kyloConfig.kafkaTemplate, kyloClient)
  private val producerService = new ProducerService(applicationConfig.kafkaConfig, payloadValidator)

  def register = Actions.basic.async(streamDataJson) { request =>
    for {
      _ <- streamService.createStreamData(request.body)
      _ <- consumerService.createConsumer(request.body)
    } yield Created
  }

  def registerCatalog(catalogId: String) = Actions.basic.securedAsync { (_, auth, _) =>
    for {
      streamData <- streamService.findStreamData(catalogId, auth)
      _          <- consumerService.createConsumer(streamData)
    } yield Created
  }

  def update(catalogId: String) = Actions.basic.securedAsync(eventJson) { (request, auth, userId) =>
    for {
      streamData <- streamService.findStreamData(catalogId, auth)
      _          <- producerService.sendMessage(streamData, userId, request.body)
    } yield Ok
  }

}
