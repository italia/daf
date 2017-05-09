package it.gov.daf.catalogmanager.service

import it.gov.daf.catalogmanager.repository.catalog.{CatalogRepositoryComponent, CatalogRepositoryDev}
import play.api.{Configuration, Environment}

/**
  * Created by ale on 08/05/17.
  */
object ServiceRegistry extends CatalogServiceComponent
  with CatalogRepositoryComponent{
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")

  val catalogRepository = new CatalogRepositoryDev
  val catalogService = new CatalogService
}

