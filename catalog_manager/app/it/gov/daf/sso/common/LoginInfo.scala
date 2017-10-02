package it.gov.daf.sso.common

class LoginInfo(userName:String, userPassword:String, applicationName:String) {

  val appName = applicationName
  val user = userName
  val password = userPassword

}
