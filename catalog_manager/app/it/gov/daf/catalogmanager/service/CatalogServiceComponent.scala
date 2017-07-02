package it.gov.daf.catalogmanager.service


import catalog_manager.yaml.{Dataset, MetaCatalog, Successf}
import it.gov.daf.catalogmanager.repository.catalog.CatalogRepositoryComponent
import it.gov.daf.catalogmanager.repository.ckan.CkanRepositoryComponent
import play.api.libs.json.JsValue

import scala.concurrent.Future

/**
  * Created by ale on 05/05/17.
  */
trait CatalogServiceComponent {
  this: CatalogRepositoryComponent with CkanRepositoryComponent =>
  val catalogService: CatalogService

  class CatalogService {

    def listCatalogs() :Seq[MetaCatalog] = {
       catalogRepository.listCatalogs()
    }
    def getCatalogs(catalogId :String) :MetaCatalog = {
      catalogRepository.getCatalogs(catalogId)
    }
    def createCatalog(metaCatalog: MetaCatalog) :Successf = {
      catalogRepository.createCatalog(metaCatalog)
    }

    // Ckan part refactor in another service

    def getDataset(catalogId :String) :Future[Dataset] = {
      ckanRepository.getDataset(catalogId)
    }

    def createDataset(jsonDataset: JsValue): Unit = {
      ckanRepository.createDataset(jsonDataset)
    }
    def dataset(datasetId: String): JsValue = {
      ckanRepository.dataset(datasetId)
    }
  }
}
