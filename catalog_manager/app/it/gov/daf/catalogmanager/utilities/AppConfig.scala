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
  val userIdHeader: Option[String] = playConfig.getString("app.userid.header")
  val ckanHost: Option[String] = playConfig.getString("app.ckan.url")
  val dbHost: Option[String] = playConfig.getString("mongo.host")
  val dbPort: Option[Int] = playConfig.getInt("mongo.port")
  val userName :Option[String] = playConfig.getString("mongo.username")
  val password :Option[String] = playConfig.getString("mongo.password")
  val database :Option[String] = playConfig.getString("mongo.database")
  val localUrl :Option[String] = playConfig.getString("app.local.url")
  val securityManHost :Option[String] = playConfig.getString("security.manager.host")
  val cookieExpiration :Option[Long] = playConfig.getLong("cookie.expiration")
  val ingestionUrl :Option[String] = playConfig.getString("ingestion.url")
  val kyloUrl: Option[String] = playConfig.getString("kylo.url")
  val kyloUser: Option[String] = playConfig.getString("kylo.user")
  val kyloPwd: Option[String] = playConfig.getString("kylo.pwd")

}



object ConfigReader {

  private val config = new AppConfig(Configuration.load(Environment.simple()))

  require(config.kyloUrl.nonEmpty, "A kylo url must be specified")
  require(config.kyloUser.nonEmpty, "A kylo user must be specified")
  require(config.kyloPwd.nonEmpty, "A kylo password must be specified")

  def userIdHeader: String = config.userIdHeader.getOrElse("userid")
  def getCkanHost: String = config.ckanHost.getOrElse("localhost")
  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
  def database :String = config.database.getOrElse("catalog_manager")
  def password :String = config.password.getOrElse("")
  def userName :String = config.userName.getOrElse("")
  def localUrl :String = config.localUrl.getOrElse("http://localhost:9001")
  def securityManHost :String = config.securityManHost.getOrElse("http://localhost:9002/security-manager")
  def cookieExpiration:Long = config.cookieExpiration.getOrElse(30L)// 30 min by default
  def ingestionUrl :String = config.ingestionUrl.getOrElse("http://localhost:9003")
  def kyloUrl: String = config.kyloUrl.get
  def kyloUser: String = config.kyloUser.get
  def kyloPwd: String = config.kyloPwd.get

}

