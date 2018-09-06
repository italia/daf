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

package client

import com.thinkbiganalytics.feedmgr.rest.model.{ FeedMetadata, NifiFeed, RegisteredTemplate }
import com.thinkbiganalytics.json.ObjectMapperSerializer
import it.gov.daf.common.utils._
import play.api.cache.CacheApi
import play.api.libs.ws.{ WSAuthScheme, WSClient }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class KyloClient(cache: CacheApi,
                 ws: WSClient,
                 host: String,
                 port: Int,
                 username: String,
                 password: String)(protected implicit val ec: ExecutionContext) {

  private val feedManagerBaseUrl = s"http://$host:$port/api/v1/feedmgr"

  private def feedRequest(relativePath: String) = ws
    .url(s"$feedManagerBaseUrl/$relativePath")
    .withAuth(username, password, WSAuthScheme.BASIC)
    .withHeaders(
      ("Content-Type", "application/json")
    )

  private def feedsRequest = feedRequest("feeds")

  private def templatesRequest = feedRequest("templates/registered")

  private def updateCache(template: RegisteredTemplate) = cache.set(s"template:[${template.getTemplateName}]", template)

  private def deleteCache(name: String) = cache.remove { s"template:[$name]" }

  private def findCachedTemplate(name: String): Option[RegisteredTemplate] = cache.get(s"template:[$name]")

  private def updateTemplate(name: String) = getTemplate(name).andThen {
    case Success(template)                  => updateCache(template)
    case Failure(_: NoSuchElementException) => deleteCache(name)
  }

  private def getTemplate(name: String): Future[RegisteredTemplate] = templatesRequest.get().flatMap {
    case response if response.status == 200 => Future.successful {
      ObjectMapperSerializer.deserialize(response.json.toString, classOf[Array[RegisteredTemplate]]).toList
    }
    case response                           => Future.failed { new RuntimeException(s"Failed to retrieve templates; reason [${response.json}]") }
  }.flatMap {
    _.find { _.getTemplateName == name }.~>[Future]
  }

  private def postFeed(feed: FeedMetadata) = feedsRequest.post { ObjectMapperSerializer.serialize(feed) }.flatMap {
    case response if response.status == 200 => Future.successful {
      ObjectMapperSerializer.deserialize(response.json.toString, classOf[NifiFeed])
    }
    case response                            => Future.failed {
      new RuntimeException(s"Failed to create feed [${feed.getFeedName}] for template [${feed.getTemplateName}]; reason [${response.json}]")
    }
  }

  // API

  def findTemplate(name: String) = findCachedTemplate(name) match {
    case None           => updateTemplate(name)
    case Some(template) => Future.successful { template }
  }

  def createFeed(feed: FeedMetadata) = postFeed(feed)

}
