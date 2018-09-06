package daf.stream.kylo

import cats.data.{ State, StateT }
import com.thinkbiganalytics.feedmgr.rest.model.{ FeedCategory, FeedMetadata }
import com.thinkbiganalytics.feedmgr.rest.model.schema.TableSetup
import com.thinkbiganalytics.nifi.rest.model.NifiProperty
import it.gov.daf.common.utils.StateFunctions
import monocle.{ Lens, PLens, Setter }

trait FeedGenerators {

  val setters = FeedState

}

private object FeedState extends StateFunctions[FeedMetadata] {

  def feedName(s: String) = set { _.setFeedName(s) }

  def feedDescription(s: String) = set { _.setDescription(s) }

  def systemFeedName(s: String) = set { _.setSystemFeedName(s) }

  def templateId(s: String) = set { _.setTemplateId(s) }

  def templateName(s: String) = set { _.setTemplateName(s) }

  def inputProcessor(processorName: String, processorType: String) = set { feed =>
    feed.setInputProcessorName(processorName)
    feed.setInputProcessorType(processorType)
  }

  def dataOwner(s: String) = set { _.setDataOwner(s) }

  def category(category: FeedCategory) = set { _.setCategory(category) }

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

private object FeedPropertiesState extends StateFunctions[NifiProperty] {

  

}
