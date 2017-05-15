package it.gov.daf.catalogmanager.repository.ckan

import catalog_manager.yaml.Dataset

import scala.concurrent.Future

/**
  * Created by ale on 10/05/17.
  */

trait CkanRepository {
  def getDataset(datasetId :String): Future[Dataset]
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
