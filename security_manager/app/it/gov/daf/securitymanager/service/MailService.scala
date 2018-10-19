package it.gov.daf.securitymanager.service

import scala.concurrent.Future
import javax.mail.internet.InternetAddress

import courier.{Envelope, Mailer, Text}
import it.gov.daf.securitymanager.utilities.ConfigReader
import security_manager.yaml.{Success,Error}


class MailService(to:String,token:String) {

  import play.api.libs.concurrent.Execution.Implicits._


  def sendRegistrationMail(): Future[Either[Error, Success]] = {

    sendMail( "Registration to DAF",
              "Click on this link to complete registration:\n",
              ConfigReader.registrationUrl) map {
                case Right(r) => Right(Success(Some("Success"), Some("ok")))
                case Left(l) => Left(Error(Option(0), Option(l), None))
              }
  }

  def sendResetPwdMail(): Future[Either[Error, Success]] = {

    sendMail( "Reset DAF password",
              "Click on this link to reset password:\n",
              ConfigReader.resetPwdUrl) map {
                case Right(r) => Right(Success(Some("Success"), Some("ok")))
                case Left(l) => Left(Error(Option(0), Option(l), None))
              }
  }

  private def sendMail(subject:String,content:String,link:String): Future[Either[String, String]] = {

    val address =
      if (MailService.SMTP_TESTMAIL != null && to.contains(MailService.SMTP_TESTMAIL))
        new InternetAddress(MailService.SMTP_SENDER)
      else
        new InternetAddress(to)


    def fut = MailService.mailer(Envelope.from(new InternetAddress(MailService.SMTP_SENDER))
      .to(address)
      .subject(subject)
      .content(Text(content + link + token)))


    fut map { _ =>
      Right("Mail sent")
    }recover{
      case e :Exception => Left(e.getMessage)
    }

  }

}

object MailService{

  private val SMTP_SERVER = ConfigReader.smtpServer
  private val SMTP_PORT = ConfigReader.smtpPort
  private val SMTP_LOGIN = ConfigReader.smtpLogin
  private val SMTP_PWD = ConfigReader.smtpPwd
  private val SMTP_SENDER = ConfigReader.smtpSender
  private val SMTP_TESTMAIL = ConfigReader.smtpTestMail

  private val mailer = Mailer(SMTP_SERVER, SMTP_PORT)
    .auth(true)
    .as(SMTP_LOGIN, SMTP_PWD)
    .startTtls(true)()


  private val SENDER = ConfigReader.smtpSender

  /*
  private val SUBJECT = "Registration to DAF"
  private val CONTENT = "Click on this link to complete registration:\n"
  private val LINK = ConfigReader.registrationUrl*/


}
