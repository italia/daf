import catalog_manager.yaml.{Dataset, MetaCatalog}
import it.gov.daf.catalogmanager.repository.catalog.CatalogRepositoryComponent
import it.gov.daf.catalogmanager.repository.catalog.CatalogRepositoryDev
import it.gov.daf.catalogmanager.repository.ckan.{CkanRepository, CkanRepositoryComponent, CkanRepositoryDev}
import it.gov.daf.catalogmanager.service.CatalogServiceComponent

import scala.concurrent.Future

//import org.scalatestplus.play._
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.specs2.mock._


import scala.collection.mutable

/**
  * Created by ale on 05/05/17.
  */

trait TestEnvironment extends
  CatalogServiceComponent with
  CatalogRepositoryComponent with
  CkanRepositoryComponent with
  Mockito
{
  val catalogRepository :CatalogRepositoryDev = mock[CatalogRepositoryDev]
  val ckanRepository: CkanRepositoryDev = mock[CkanRepositoryDev]
  val catalogService :CatalogService = new CatalogService
  //mock[CatalogService]


  catalogRepository.catalog("anything") returns None
  //catalogRepository.listCatalogs() returns Seq()
}



class CatalogServiceSpec extends Specification with TestEnvironment {

  "A CatalogService" should  {
    "catalogService.getCatalogs() return MetaCatalog" in {
      val catalog: Option[MetaCatalog] = catalogService.catalog("anything")
      println(catalog)
      catalog must be equalTo  None
    }
    /*
    "catalogService.listCatalogs return a list of MetaCatalog" in {
      //val catalog = catalogService.listCatalogs
      //catalog must be equalTo Seq()
    }
*/
  }
}
