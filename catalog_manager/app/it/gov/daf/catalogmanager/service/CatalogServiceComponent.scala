package it.gov.daf.catalogmanager.service


import catalog_manager.yaml.{Dataset, MetaCatalog, Successf}
import it.gov.daf.catalogmanager.repository.catalog.CatalogRepositoryComponent
import it.gov.daf.catalogmanager.repository.ckan.CkanRepositoryComponent

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

    def getDataset(catalogId :String) :Future[Dataset] = {
      ckanRepository.getDataset(catalogId)
    }
  }
}
