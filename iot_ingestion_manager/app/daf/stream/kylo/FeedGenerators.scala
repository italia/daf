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

import cats.data.State
import cats.instances.list.catsKernelStdMonoidForList
import com.thinkbiganalytics.feedmgr.rest.model.{ FeedCategory, FeedMetadata, RegisteredTemplate, UserProperty }
import com.thinkbiganalytics.nifi.rest.model.NifiProperty
import it.gov.daf.common.utils._
import representation.{ KafkaSource, StreamData }

import scala.collection.convert.decorateAsScala._
import scala.collection.convert.decorateAsJava._

trait FeedGenerators { this: KyloConsumerService =>

  // Add config:
  //   - kafka offset reset
  //   - default owner

  private def topic(streamData: StreamData) = streamData.source match {
    case kafka: KafkaSource => kafka.topic
    case _                  => throw new IllegalArgumentException("Unsupported stream type encountered; only [kafka] is supported")
  }

  private def properties(template: RegisteredTemplate, streamData: StreamData) = for {
    _ <- FeedPropertiesState.init(template)
    _ <- FeedPropertiesState.property("auto.offset.reset", kafkaConfig.offsetReset)
    _ <- FeedPropertiesState.property("bootstrap.servers", kafkaConfig.servers mkString ",")
    _ <- FeedPropertiesState.property("group.id"         , kafkaConfig.groupId)
    _ <- FeedPropertiesState.property("topic"            , topic(streamData))
  } yield ()

  private def userProperties(streamData: StreamData) = for {
    _ <- UserPropertyState.add("daf_type"     , 1) { if (streamData.isStandard) "standard" else "ordinary" }
    _ <- UserPropertyState.add("daf_domain"   , 2) { streamData.domain }
    _ <- UserPropertyState.add("daf_subdomain", 3) { streamData.subDomain }
    _ <- UserPropertyState.add("daf_opendata" , 4) { streamData.isOpenData }
    _ <- UserPropertyState.add("daf_owner"    , 5) { streamData.group }
  } yield ()

  private def inputProcessor(template: RegisteredTemplate) = template.getInputProcessors.asScala.headOption match {
    case Some(processor) => processor
    case None            => throw new IllegalArgumentException(s"No input processors found in template [${template.getId}] (${template.getTemplateName})")
  }


  private def state(streamData: StreamData, template: RegisteredTemplate, category: FeedCategory) = for {
    _ <- FeedState.feedName        { streamData.name }
    _ <- FeedState.feedDescription { streamData.description }
    _ <- FeedState.systemFeedName  { streamData.name }
    _ <- FeedState.dataOwner       { streamData.owner }
    _ <- FeedState.templateId      { template.getId }
    _ <- FeedState.templateName    { template.getTemplateName }
    _ <- FeedState.category        { category }
    _ <- FeedState.active          { true }
    _ <- FeedState.inputProcessor  { inputProcessor(template) }
    _ <- State.get[FeedMetadata].set(userProperties(streamData))       { (feed, props) => feed.setUserProperties(props.toSet.asJava) }
    _ <- State.get[FeedMetadata].set(properties(template, streamData)) { (feed, props) => feed.setProperties(props.asJava) }
  } yield ()

  def init(streamData: StreamData, template: RegisteredTemplate, category: FeedCategory) = state(streamData, template, category).runS { new FeedMetadata }.value

}

private object FeedState extends StateFunctions[FeedMetadata] {

  def feedName(s: String) = set { _.setFeedName(s) }

  def feedDescription(s: String) = set { _.setDescription(s) }

  def systemFeedName(s: String) = set { _.setSystemFeedName(s) }

  def templateId(s: String) = set { _.setTemplateId(s) }

  def templateName(s: String) = set { _.setTemplateName(s) }

  def inputProcessor(processorName: String, processorType: String): State[FeedMetadata, Unit] = set { feed =>
    feed.setInputProcessorName(processorName)
    feed.setInputProcessorType(processorType)
  }

  def inputProcessor(processor: RegisteredTemplate.Processor): State[FeedMetadata, Unit] = inputProcessor(
    processor.getName,
    processor.getType
  )

  def dataOwner(s: String) = set { _.setDataOwner(s) }

  def category(category: FeedCategory) = set { _.setCategory(category) }

  def properties(properties: Set[NifiProperty]) = set { _.setProperties(properties.toList.asJava) }

  def userProperties(properties: Set[UserProperty]) = set { _.setUserProperties(properties.asJava) }

  def active(bool: Boolean) = set { _.setActive(true) }

}

private object FeedCategoryState extends StateFunctions[FeedCategory] {

  def id(s: String) = set { _.setId(s) }

  def name(s: String) = set { _.setName(s) }

  def systemName(s: String) = set { _.setSystemName(s) }

  def init(categoryId: String, categoryName: String, categorySystemName: String): State[FeedCategory, FeedCategory] = for {
    _        <- id(categoryId)
    _        <- name(categoryName)
    _        <- systemName(categorySystemName)
    category <- State.get
  } yield category

}

private object FeedPropertiesState extends StateFunctions[List[NifiProperty]] {

  def init(template: RegisteredTemplate) = State.set {
    template.getProperties.asScala.filter { _.isUserEditable }.toList
  }

  def property(name: String, value: String) = set {
    _.find { _.getKey == name }.foreach { _.setValue(value) }
  }

}

private object UserPropertyState extends StateFunctions[UserProperty] {

  def add(name: String, order: Int)(value: Any) = State[List[UserProperty], Unit] { tail =>
    { init(name, value.toString, order).runS { new UserProperty }.value :: tail } -> (())
  }

  def init(name: String, value: String, order: Int) = for {
    _ <- set { _.setSystemName(name) }
    _ <- set { _.setValue(value) }
    _ <- set { _.setOrder(order) }
  } yield ()

}
