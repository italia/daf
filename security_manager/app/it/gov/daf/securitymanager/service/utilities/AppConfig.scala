package it.gov.daf.securitymanager.service.utilities

import javax.inject.Inject

import play.api.{Configuration, Environment}


private class AppConfig @Inject()(playConfig: Configuration) {

  //val userIdHeader: Option[String] = playConfig.getString("app.userid.header")
  val ckanHost: Option[String] = playConfig.getString("ckan.url")
  val ckanAdminUser:Option[String] = playConfig.getString("ckan.adminUser")
  val ckanAdminPwd:Option[String] = playConfig.getString("ckan.adminPwd")
  val ckanGeoHost: Option[String] = playConfig.getString("ckan-geo.url")
  val ckanGeoAdminUser:Option[String] = playConfig.getString("ckan-geo.adminUser")
  val ckanGeoAdminPwd:Option[String] = playConfig.getString("ckan-geo.adminPwd")
  val dbHost: Option[String] = playConfig.getString("mongo.host")
  val dbPort: Option[Int] = playConfig.getInt("mongo.port")
  val userName :Option[String] = playConfig.getString("mongo.username")
  val password :Option[String] = playConfig.getString("mongo.password")
  val database :Option[String] = playConfig.getString("mongo.database")
  //val localUrl :Option[String] = playConfig.getString("app.local.url")

  val registrationUrl :Option[String] = playConfig.getString("app.registration.url")
  val resetPwdUrl :Option[String] = playConfig.getString("app.resetpwd.url")
  val ipaUrl :Option[String] = playConfig.getString("ipa.url")
  val ipaUser :Option[String] = playConfig.getString("ipa.user")
  val ipaUserPwd :Option[String] = playConfig.getString("ipa.userpwd")
  val smtpServer :Option[String] = playConfig.getString("smtp.server")
  val smtpPort: Option[Int] = playConfig.getInt("smtp.port")
  val smtpLogin :Option[String] = playConfig.getString("smtp.login")
  val smtpPwd :Option[String] = playConfig.getString("smtp.pwd")
  val smtpSender:Option[String] = playConfig.getString("smtp.sender")
  val smtpTestMail:Option[String] = playConfig.getString("smtp.testMail")

  val supersetUrl :Option[String] = playConfig.getString("superset.url")
  val suspersetAdminUser:Option[String] = playConfig.getString("superset.adminUser")
  val suspersetAdminPwd:Option[String] = playConfig.getString("superset.adminPwd")
  val suspersetOrgAdminRole:Option[String] = playConfig.getString("superset.orgAdminRole")
  val suspersetDbUri:Option[String] = playConfig.getString("superset.dbUri")


  val metabaseUrl :Option[String] = playConfig.getString("metabase.url")
  val metabaseAdminUser :Option[String] = playConfig.getString("metabase.adminUser")
  val metabaseAdminPwd :Option[String] = playConfig.getString("metabase.adminPwd")

  val jupyterUrl :Option[String] = playConfig.getString("jupyter.url")

  val grafanaUrl :Option[String] = playConfig.getString("grafana.url")
  val grafanaAdminUser :Option[String] = playConfig.getString("grafana.adminUser")
  val grafanaAdminPwd :Option[String] = playConfig.getString("grafana.adminPwd")

  val tokenExpiration :Option[Long] = playConfig.getLong("token.expiration")
  val cookieExpiration :Option[Long] = playConfig.getLong("cookie.expiration")

  //val defaultOrganization:Option[String] = playConfig.getString("default.organization")

  val kyloUrl :Option[String] = playConfig.getString("kylo.url")
  val kyloUser :Option[String] = playConfig.getString("kylo.user")
  val kyloUserPwd :Option[String] = playConfig.getString("kylo.userpwd")

  val hadoopUrl :Option[String] = playConfig.getString("hadoop.url")

  val impalaServer :Option[String] = playConfig.getString("impala.server")
  val impalaKeyStorePath :Option[String] = playConfig.getString("impala.keyStorePath")
  val impalaKeyStorePwd :Option[String] = playConfig.getString("impala.keyStorePwd")
  val impalaAdminUser :Option[String] = playConfig.getString("impala.adminUser")
  val impalaAdminUserPwd :Option[String] = playConfig.getString("impala.adminUserPwd")

  val localEnv:Option[Boolean] = playConfig.getBoolean("localEnv")

}


object ConfigReader {

  private val config = new AppConfig(Configuration.load(Environment.simple()))

  //require(config.defaultOrganization.nonEmpty,"A default organization must be specified")
  require(config.suspersetAdminUser.nonEmpty,"A superset admin must be specified")
  require(config.suspersetAdminPwd.nonEmpty,"A superset admin password must be specified")
  require(config.suspersetOrgAdminRole.nonEmpty,"A superset organization admin role must be specified")
  require(config.suspersetDbUri.nonEmpty,"A superset db uri must be specified")
  require(config.ckanHost.nonEmpty,"A ckan host must be specified")
  require(config.ckanAdminUser.nonEmpty,"A ckan admin must be specified")
  require(config.ckanAdminPwd.nonEmpty,"A ckan admin password must be specified")
  require(config.ckanGeoHost.nonEmpty,"A ckan geo host must be specified")
  require(config.ckanGeoAdminUser.nonEmpty,"A ckan geo admin must be specified")
  require(config.ckanGeoAdminPwd.nonEmpty,"A ckan geo  admin password must be specified")
  require(config.grafanaAdminUser.nonEmpty,"A grafana admin must be specified")
  require(config.grafanaAdminPwd.nonEmpty,"A grafana admin password must be specified")

  require(config.registrationUrl.nonEmpty,"A registration url must be specified")
  require(config.resetPwdUrl.nonEmpty,"A reset password url must be specified")

  require(config.kyloUrl.nonEmpty,"A kylo url must be specified")
  require(config.kyloUser.nonEmpty,"A kylo user must be specified")
  require(config.kyloUserPwd.nonEmpty,"A kylo user password must be specified")

  require(config.metabaseUrl.nonEmpty,"Metabase url must be specified")
  require(config.metabaseAdminUser.nonEmpty,"Metabase user must be specified")
  require(config.metabaseAdminPwd.nonEmpty,"Metabase password must be specified")

  require(config.hadoopUrl.nonEmpty,"Hadoop url must be specified")

  require(config.impalaServer.nonEmpty,"Impala server must be specified")
  require(config.impalaKeyStorePath.nonEmpty,"Impala KeyStore path must be specified")
  require(config.impalaKeyStorePwd.nonEmpty,"Impala KeyStore pwd must be specified")

  require(config.impalaAdminUser.nonEmpty,"Impala admin user must be specified")
  require(config.impalaAdminUserPwd.nonEmpty,"Impala admin user pwd must be specified")


  //def userIdHeader: String = config.userIdHeader.getOrElse("userid")
  def ckanHost: String = config.ckanHost.get
  def ckanAdminUser:String = config.ckanAdminUser.get
  def ckanAdminPwd:String = config.ckanAdminPwd.get

  def ckanGeoHost: String = config.ckanGeoHost.get
  def ckanGeoAdminUser:String = config.ckanGeoAdminUser.get
  def ckanGeoAdminPwd:String = config.ckanGeoAdminPwd.get

  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
  def database :String = config.database.getOrElse("security_manager")
  def password :String = config.password.getOrElse("")
  def userName :String = config.userName.getOrElse("")

  def registrationUrl :String = config.registrationUrl.get
  def resetPwdUrl :String = config.resetPwdUrl.get
  def ipaUrl :String = config.ipaUrl.getOrElse("xxx")
  def ipaUser :String = config.ipaUser.getOrElse("xxx")
  def ipaUserPwd :String = config.ipaUserPwd.getOrElse("xxx")
  def smtpServer :String = config.smtpServer.getOrElse("xxx")
  def smtpPort: Int = config.smtpPort.getOrElse(0)
  def smtpLogin :String = config.smtpLogin.getOrElse("xxx")
  def smtpPwd :String = config.smtpPwd.getOrElse("xxx")
  def smtpTestMail:String = config.smtpTestMail.getOrElse(null)
  def smtpSender:String = config.smtpSender.getOrElse("xxx")

  def supersetUrl:String = config.supersetUrl.getOrElse("xxx")
  def suspersetAdminUser:String = config.suspersetAdminUser.get
  def suspersetAdminPwd:String = config.suspersetAdminPwd.get
  def suspersetOrgAdminRole:String = config.suspersetOrgAdminRole.get
  def suspersetDbUri:String = config.suspersetDbUri.get

  def metabaseUrl:String = config.metabaseUrl.get
  def metabaseAdminUser:String = config.metabaseAdminUser.get
  def metabaseAdminPwd:String = config.metabaseAdminPwd.get

  def jupyterUrl:String = config.jupyterUrl.getOrElse("xxx")

  def grafanaUrl:String = config.grafanaUrl.getOrElse("xxx")
  def grafanaAdminUser:String = config.grafanaAdminUser.get
  def grafanaAdminPwd:String = config.grafanaAdminPwd.get

  def tokenExpiration:Long = config.tokenExpiration.getOrElse(60L*8L)// 8h by default
  def cookieExpiration:Long = config.cookieExpiration.getOrElse(30L)// 30 min by default

  //def defaultOrganization:String = config.defaultOrganization.get

  def kyloUrl :String = config.kyloUrl.get
  def kyloUser :String = config.kyloUser.get
  def kyloUserPwd :String = config.kyloUserPwd.get

  def hadoopUrl :String = config.hadoopUrl.get

  def impalaServer :String = config.impalaServer.get
  def impalaKeyStorePath :String = config.impalaKeyStorePath.get
  def impalaKeyStorePwd :String = config.impalaKeyStorePwd.get
  def impalaAdminUser :String = config.impalaAdminUser.get
  def impalaAdminUserPwd :String = config.impalaAdminUserPwd.get

  def localEnv:Boolean = config.localEnv.getOrElse(false)

}

