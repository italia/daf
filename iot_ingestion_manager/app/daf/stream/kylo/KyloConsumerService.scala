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

package daf.stream.kylo

import client.KyloClient
import com.thinkbiganalytics.feedmgr.rest.model.NifiFeed
import config.KafkaConfig
import daf.stream.ConsumerService
import daf.stream.error.StreamCreationError
import org.slf4j.LoggerFactory
import representation.StreamData

import scala.collection.convert.decorateAsScala._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class KyloConsumerService(val kafkaConfig: KafkaConfig,
                          kyloTemplateName: String,
                          kyloClient: KyloClient)(implicit executionContext: ExecutionContext) extends ConsumerService with FeedGenerators {

  private val logger = LoggerFactory.getLogger("it.gov.daf.iot.Kylo")

  private val logFeedCreation: PartialFunction[Try[NifiFeed], Unit] = {
    case Success(feed)  => logger.info { s"Created feed with id [${feed.getFeedMetadata.getFeedId}] and name [${feed.getFeedMetadata.getFeedName}]" }
    case Failure(error) => logger.error("Error creating feed", error)
  }

  private def checkFeedCreation(feed: NifiFeed) = if (feed.getErrorMessages.isEmpty) Future.successful(feed) else Future.failed {
    StreamCreationError { s"Failed to create feed with id [${feed.getFeedMetadata.getFeedId}] - reason: \n [${feed.getErrorMessages.asScala.mkString("\n")}]" }
  }

  def createConsumer(streamData: StreamData) = for {
    category <- kyloClient.findCategory(streamData.owner)
    template <- kyloClient.findTemplate(kyloTemplateName)
    feed     <- Future.successful { init(streamData, template, category) }
    result   <- kyloClient.createFeed(feed)
    _        <- checkFeedCreation(result).andThen { logFeedCreation }
  } yield ()

}
