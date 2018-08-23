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

import com.thinkbiganalytics.feedmgr.rest.model.FeedMetadata
import com.thinkbiganalytics.nifi.rest.model.NifiProperty
import daf.instances.{ ConfigurationInstance, PlayInstance }
import daf.kylo.KyloRouting
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpec }

import scala.collection.convert.decorateAsJava._
import scala.concurrent.Await
import scala.concurrent.duration._

class KyloClientSpec extends WordSpec
  with MustMatchers
  with ConfigurationInstance
  with PlayInstance
  with BeforeAndAfterAll
  with KyloRouting {

  private lazy val kyloClient = new KyloClient(
    cache        = cache,
    ws           = ws,
    host         = "localhost",
    port         = serverPort,
    username     = "test-admin",
    password     = "test-password"
  )

  override def beforeAll() = {
    startPlay()
  }

  override def afterAll() = {
    stopPlay()
  }

  "A KyloClient" must {

    "create feeds" in {
      Await.result(
        kyloClient.createFeed { KyloFeedData.feed },
        5.seconds
      ).getFeedMetadata.getFeedId must be { KyloFeedData.feed.getId }
    }

    "retrieve templates" in {
      Await.result(
        kyloClient.findTemplate("test-template-1"),
        5.seconds
      ).getTemplateName must be { "test-template-1" }
    }

  }

}

object KyloFeedData {

  private def nifiProperty(key: String, value: String) = new NifiProperty("test-processor-group", "test-processor-id", key, value)

  private def configureFeed(feed: FeedMetadata = new FeedMetadata) = {
    feed.setTemplateId   { "test-template-id"   }
    feed.setTemplateName { "test-template-name" }
    feed.setFeedName     { "test-feed-name"     }
    feed.setProperties {
      { 0 until 10 }.map { i => nifiProperty(s"key-$i", s"value-$i") }.asJava
    }
    feed
  }

  val feed = configureFeed()

}
