package it.gov.daf.play.security

object protocol {
  sealed trait ServiceCredentials {
    def username: String
    def password: Option[String]
    def appName: String
  }

  case class CkanCredentials(
    username: String,
    password: Option[String] = None,
    appName: String = "ckan"
  ) extends ServiceCredentials


}
