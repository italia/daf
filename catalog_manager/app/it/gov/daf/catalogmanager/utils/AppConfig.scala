package it.gov.daf.catalogmanager.utils

/**
  * Created by ale on 11/05/17.
  */

import javax.inject.Inject

import play.api.{Configuration, Environment}

/**
  * Created by ale on 16/04/17.
  */
private class AppConfig @Inject()(playConfig: Configuration) {
  val ckanHost: Option[String] = playConfig.getString("")

}

object ConfigReader {
  private val config = new AppConfig(Configuration.load(Environment.simple()))
  def getCkanHost: String = config.ckanHost.getOrElse("app.ckan.url")
}
