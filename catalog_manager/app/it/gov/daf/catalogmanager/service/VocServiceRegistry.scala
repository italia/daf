package it.gov.daf.catalogmanager.service

import it.gov.daf.catalogmanager.repository.voc.{VocThemeRepository, VocThemeRepositoryComponent}
import play.api.{Configuration, Environment}


object VocServiceRegistry extends VocThemeRepositoryComponent{
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val vocRepository =  VocThemeRepository(app)
}
