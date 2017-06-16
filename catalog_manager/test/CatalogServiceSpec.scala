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


  catalogRepository.getCatalogs("anything") returns MetaCatalog(None,None,None)
  catalogRepository.listCatalogs() returns Seq(MetaCatalog(None,None,None))
}



class CatalogServiceSpec extends Specification with TestEnvironment {

  "A CatalogService" should  {
    "catalogService.getCatalogs() return MetaCatalog" in {
      val catalog :MetaCatalog = catalogService.getCatalogs("anything")
      println(catalog)
      catalog must be equalTo  MetaCatalog(None,None,None)
    }
    "catalogService.listCatalogs return a list of MetaCatalog" in {
      val catalog = catalogService.listCatalogs()(0)
      catalog must be equalTo MetaCatalog(None,None,None)
    }

  }
}
