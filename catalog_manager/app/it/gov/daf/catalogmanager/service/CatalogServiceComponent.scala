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

    def listCatalogs: Seq[MetaCatalog] = {
       catalogRepository.listCatalogs()
    }
    def catalog(catalogId :String): Option[MetaCatalog] = {
      catalogRepository.catalog(catalogId)
    }
    def createCatalog(metaCatalog: MetaCatalog, callingUserid :MetadataCat): Success = {
      catalogRepository.createCatalog(metaCatalog, callingUserid)
    }
  }
}
