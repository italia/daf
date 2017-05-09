package it.gov.daf.catalogmanager.service

import catalog_manager.yaml.{MetaCatalog, Successf}
import it.gov.daf.catalogmanager.repository.catalog.CatalogRepositoryComponent

/**
  * Created by ale on 05/05/17.
  */
trait CatalogServiceComponent {
  this: CatalogRepositoryComponent =>
  val catalogService: CatalogService

  class CatalogService {

    def listCatalogs() :Seq[MetaCatalog] = {
       catalogRepository.listCatalogs()
    }
    def getCatalogs(catalogId :String) :MetaCatalog = {
      catalogRepository.getCatalogs(catalogId)
    }
    def createCatalog() :Successf = {
      catalogService.createCatalog()
    }
  }
}
