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
import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.common.utils._

import scala.collection.convert.decorateAsScala._
import scala.collection.convert.decorateAsJava._

trait FeedGenerators {

  // Add config:
  //   - kafka offset reset
  //   - default owner

  private def name(catalog: MetaCatalog) = catalog.dcatapit.holder_identifier.map { s => s"${s}_o_${catalog.dcatapit.name}" } getOrElse catalog.dcatapit.name

  private def owner(catalog: MetaCatalog) = catalog.dcatapit.owner_org getOrElse "anonymous"

  private def properties(template: RegisteredTemplate) = for {
    _ <- FeedPropertiesState.init(template)
    _ <- FeedPropertiesState.property("auto.offset.reset", "latest")
  } yield ()

  private def userProperties(catalog: MetaCatalog) = for {
    _ <- UserPropertyState.add("daf_type"     , 1) { if (catalog.operational.is_std) "standard" else "ordinary" }
    _ <- UserPropertyState.add("daf_domain"   , 2) { catalog.operational.theme }
    _ <- UserPropertyState.add("daf_subdomain", 3) { catalog.operational.subtheme }
    _ <- UserPropertyState.add("daf_opendata" , 4) {!catalog.dcatapit.privatex.getOrElse(false) }
    _ <- UserPropertyState.add("daf_owner"    , 5) { catalog.operational.group_own }
  } yield ()

  private def inputProcessor(template: RegisteredTemplate) = template.getInputProcessors.asScala.headOption match {
    case Some(processor) => processor
    case None            => throw new IllegalArgumentException(s"No input processors found in template [${template.getId}] (${template.getTemplateName})")
  }


  private def state(catalog: MetaCatalog, template: RegisteredTemplate, category: FeedCategory) = for {
    _ <- FeedState.feedName        { name(catalog) }
    _ <- FeedState.feedDescription { catalog.dcatapit.notes }
    _ <- FeedState.systemFeedName  { name(catalog) }
    _ <- FeedState.templateId      { template.getId }
    _ <- FeedState.templateName    { template.getTemplateName }
    _ <- FeedState.dataOwner       { owner(catalog) }
    _ <- FeedState.category        { category }
    _ <- FeedState.active          { true }
    _ <- FeedState.inputProcessor  { inputProcessor(template) }
    _ <- State.get[FeedMetadata].set(userProperties(catalog)) { (feed, props) => feed.setUserProperties(props.toSet.asJava) }
    _ <- State.get[FeedMetadata].set(properties(template))    { (feed, props) => feed.setProperties(props.asJava) }
  } yield ()

  def init(catalog: MetaCatalog, template: RegisteredTemplate, category: FeedCategory) = state(catalog, template, category).runS { new FeedMetadata }.value

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
