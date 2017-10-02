package it.gov.daf.catalogmanager.service


import catalog_manager.yaml.{Dataset, MetaCatalog, MetadataCat, Success}
import it.gov.daf.catalogmanager.repository.catalog.CatalogRepositoryComponent
import play.api.libs.json.JsValue

import scala.concurrent.Future

/**
  * Created by ale on 05/05/17.
  */
trait CatalogServiceComponent {
  this: CatalogRepositoryComponent  =>
  val catalogService: CatalogService

  class CatalogService {

    def listCatalogs(page :Option[Int], limit :Option[Int]) :Seq[MetaCatalog] = {
       catalogRepository.listCatalogs(page, limit)
    }
    def getCatalogs(catalogId :String) :MetaCatalog = {
      catalogRepository.getCatalogs(catalogId)
    }
    def createCatalog(metaCatalog: MetaCatalog, callingUserid :MetadataCat) :Success = {
      println("Service : " +  callingUserid)
      catalogRepository.createCatalog(metaCatalog, callingUserid)
    }
  }
}
