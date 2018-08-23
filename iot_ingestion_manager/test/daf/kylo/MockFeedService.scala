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

package daf.kylo

import com.thinkbiganalytics.feedmgr.rest.controller.FeedRestController
import com.thinkbiganalytics.feedmgr.rest.model.{ FeedMetadata, NifiFeed, RegisteredTemplate }
import com.thinkbiganalytics.nifi.rest.model.{ NifiProcessGroup, NifiProperty }
import daf.util.MockService
import org.mockito.InjectMocks

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.convert.decorateAsJava._
import scala.util.Success

class MockFeedService(templates: Map[String, RegisteredTemplate] = Map.empty[String, RegisteredTemplate]) extends MockService {

  @InjectMocks
  private var feedController: FeedRestController = _

  private var feeds: Map[String, NifiFeed] = Map.empty[String, NifiFeed]

  private def addFeed(nifiFeed: NifiFeed) = feeds += { nifiFeed.getFeedMetadata.getId -> nifiFeed }

  private def makeFeed(feedMetadata: FeedMetadata) = new NifiFeed(feedMetadata, new NifiProcessGroup())

  def createFeed(feedMetadata: FeedMetadata) = Future.successful { makeFeed(feedMetadata) }.andThen {
    case Success(nifiFeed) => addFeed(nifiFeed)
  }.map { ok(_) }.recover {
    case throwable => error { s"Unable to create feed [${feedMetadata.getId}] - ${throwable.getMessage}" }
  }

  def registeredTemplates = ok { templates.values.toList.asJava }

}

object MockFeedService {

  private def makeProperty(name: String, key: String, value: String, userEditable: Boolean = true) = {
    val property = new NifiProperty(s"$name-group", s"$name-processor", key, value)
    property.setUserEditable(userEditable)
    property
  }

  private def makeTemplate(name: String, template: RegisteredTemplate = new RegisteredTemplate) = {
    template.setTemplateName   { name           }
    template.setId { s"$name-id"    }
    template.setProperties {
      (1 to 5).map { i => makeProperty(name, s"key-$i", s"value-$i") }.toList.asJava
    }
    template.getId -> template
  }

  def init(numTemplates: Int = 5) = new MockFeedService(
    (1 to numTemplates).map { i => makeTemplate(s"test-template-$i") }.toMap[String, RegisteredTemplate]
  )

}


