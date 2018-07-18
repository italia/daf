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
import com.google.inject.Inject
import config.StreamApplicationConfig
import daf.stream.StreamService
import it.gov.daf.common.web.{ Actions, SecureController }
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc.BodyParsers
import representation.{ KafkaStreamData, SocketStreamData, StreamData }
import representation.json.streamDataReader

import scala.util.{ Failure, Success }

class StreamController @Inject()(configuration: Configuration, playSessionStore: PlaySessionStore) extends SecureController(configuration, playSessionStore) with StreamAPI {

  private val streamDataJson = BodyParsers.parse.json[StreamData](streamDataReader)

  private val applicationConfig = StreamApplicationConfig.reader.read { configuration } match {
    case Success(config) => config
    case Failure(error)  => throw new RuntimeException("Unable to configure [iot-manager]", error)
  }

  private val streamService = new StreamService(applicationConfig.kafkaConfig)

  private def createSocketStream(id: String, interval: Int, port: Int) = streamService.createSocket(id, interval, port).map {
    case query if query.isActive => Created
    case _                       => InternalServerError { s"Query with id [$id] failed to start" }
  }

  def create = Actions.basic.attempt(streamDataJson) { request =>
    request.body match {
      case _: KafkaStreamData                   => Success { NotImplemented }
      case SocketStreamData(id, interval, port) => createSocketStream(id, interval, port)
    }
  }

}
