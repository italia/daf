package it.gov.daf.catalogmanager.utilities

/**
  * Created by ale on 11/05/17.g
  */

import javax.inject.Inject

import play.api.{Configuration, Environment}

/**
  * Created by ale on 16/04/17.
  */
private class AppConfig @Inject()(playConfig: Configuration) {
  val ckanHost: Option[String] = playConfig.getString("app.ckan.url")
  val dbHost: Option[String] = playConfig.getString("mongo.host")
  val dbPort: Option[Int] = playConfig.getInt("mongo.port")
}


object ConfigReader {
  private val config = new AppConfig(Configuration.load(Environment.simple()))
  def getCkanHost: String = config.ckanHost.getOrElse("localhost")
  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
}

