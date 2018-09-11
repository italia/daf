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

import com.thinkbiganalytics.feedmgr.rest.model.{ FeedCategory, FeedMetadata, NifiFeed, RegisteredTemplate }
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
      "Content-Type" -> "application/json"
    )

  private def feedsRequest = feedRequest("feeds")

  private def templatesRequest = feedRequest("templates/registered")

  private def categoriesRequest = feedRequest("categories")

  // CACHE - Template

  private def updateCachedTemplate(template: RegisteredTemplate) = cache.set(s"template:[${template.getTemplateName}]", template)

  private def deleteCachedTemplate(name: String) = cache.remove { s"template:[$name]" }

  private def findCachedTemplate(name: String): Option[RegisteredTemplate] = cache.get(s"template:[$name]")

  // CACHE - Category

  private def updateCachedCategory(category: FeedCategory) = cache.set(s"category:[${category.getSystemName}]", category)

  private def deleteCachedCategory(systemName: String) = cache.remove { s"category:[$systemName]" }

  private def findCachedCategory(systemName: String): Option[FeedCategory] = cache.get(s"category:[$systemName]")

  // Calls

  private def updateTemplate(name: String) = getTemplate(name).andThen {
    case Success(template)                  => updateCachedTemplate(template)
    case Failure(_: NoSuchElementException) => deleteCachedTemplate(name)
  }

  private def updateCategory(systemName: String) = getCategory(systemName).andThen {
    case Success(category)                  => updateCachedCategory(category)
    case Failure(_: NoSuchElementException) => deleteCachedCategory(systemName)
  }

  private def getTemplate(name: String): Future[RegisteredTemplate] = templatesRequest.get().flatMap {
    case response if response.status == 200 => Future.successful {
      ObjectMapperSerializer.deserialize(response.json.toString, classOf[Array[RegisteredTemplate]]).toList
    }
    case response                           => Future.failed { new RuntimeException(s"Failed to retrieve templates; reason [${response.json}]") }
  }.flatMap {
    _.find { _.getTemplateName == name }.~>[Future]
  }

  private def getCategory(systemName: String): Future[FeedCategory] = categoriesRequest.get().flatMap {
    case response if response.status == 200 => Future.successful {
      ObjectMapperSerializer.deserialize(response.json.toString, classOf[Array[FeedCategory]]).toList
    }
    case response                           => Future.failed { new RuntimeException(s"Failed to retrieve templates; reason [${response.json}]") }
  }.flatMap {
    _.find { _.getSystemName == systemName }.~>[Future]
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

  def findCategory(systemName: String) = findCachedCategory(systemName) match {
    case None           => updateCategory(systemName)
    case Some(category) => Future.successful { category }
  }

  def createFeed(feed: FeedMetadata) = postFeed(feed)

}
