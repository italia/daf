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

  val registrationUrl :Option[String] = playConfig.getString("app.registration.url")
  val ipaUrl :Option[String] = playConfig.getString("ipa.url")
  val ipaUser :Option[String] = playConfig.getString("ipa.user")
  val ipaUserPwd :Option[String] = playConfig.getString("ipa.userpwd")
  val smtpServer :Option[String] = playConfig.getString("smtp.server")
  val smtpPort: Option[Int] = playConfig.getInt("smtp.port")
  val smtpLogin :Option[String] = playConfig.getString("smtp.login")
  val smtpPwd :Option[String] = playConfig.getString("smtp.pwd")

}



object ConfigReader {
  private val config = new AppConfig(Configuration.load(Environment.simple()))
  def userIdHeader: String = config.userIdHeader.getOrElse("userid")
  def getCkanHost: String = config.ckanHost.getOrElse("localhost")
  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
  def database :String = config.database.getOrElse("catalog_manager")
  def password :String = config.password.getOrElse("")
  def userName :String = config.userName.getOrElse("")
  def localUrl :String = config.localUrl.getOrElse("http://localhost:9001")

  def registrationUrl :String = config.registrationUrl.getOrElse("http://localhost:3000/confirmregistration?t=")
  def ipaUrl :String = config.ipaUrl.getOrElse("xxx")
  def ipaUser :String = config.ipaUser.getOrElse("xxx")
  def ipaUserPwd :String = config.ipaUserPwd.getOrElse("xxx")
  def smtpServer :String = config.smtpServer.getOrElse("xxx")
  def smtpPort: Int = config.smtpPort.getOrElse(0)
  def smtpLogin :String = config.smtpLogin.getOrElse("xxx")
  def smtpPwd :String = config.smtpPwd.getOrElse("xxx")
}

