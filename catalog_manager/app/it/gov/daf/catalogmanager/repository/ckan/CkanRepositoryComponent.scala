package it.gov.daf.catalogmanager.repository.ckan

import catalog_manager.yaml.Dataset
import play.api.libs.json.JsValue

import scala.concurrent.Future

/**
  * Created by ale on 10/05/17.
  */

trait CkanRepository {
  def getDataset(datasetId :String): Future[Dataset]

  // See how to refactor this
  def createDataset(jsonDataset: JsValue): Unit
  def dataset(datasetId: String): JsValue

}

trait CkanRepositoryComponent {
  val ckanRepository: CkanRepository
}

object CkanRepository {
  def apply(config: String):CkanRepository = config match {
    case "dev" => new CkanRepositoryDev
    case "prod" => new CkanRepositoryProd
  }
}
