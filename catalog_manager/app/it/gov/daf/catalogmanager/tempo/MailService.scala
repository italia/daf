package it.gov.daf.catalogmanager.tempo

import javax.mail.internet.InternetAddress

import courier.{Envelope, Mailer, Text}
import it.gov.daf.catalogmanager.utilities.ConfigReader

import scala.concurrent.{Await, Future}


class MailService(to:String,token:String) {

  import scala.concurrent.ExecutionContext.Implicits._

  def sendMail():Future[Either[String,String]]={

   // def fut =
   def fut = MailService.mailer(Envelope.from(new InternetAddress(MailService.SENDER))
    .to(new InternetAddress(to))
    //.cc(new InternetAddress("dad@gmail.com") )
    .subject(MailService.SUBJECT)
    .content(Text(MailService.CONTENT+MailService.LINK+token)))

    fut map { _ =>
      Right("Mail sent")
    }


  }

}


object MailService{

  private val SMTP_SERVER = ConfigReader.smtpServer
  private val SMTP_PORT = ConfigReader.smtpPort
  private val SMTP_LOGIN = ConfigReader.smtpLogin
  private val SMTP_PWD = ConfigReader.smtpPwd

  private val mailer = Mailer(SMTP_SERVER, SMTP_PORT)
    .auth(true)
    .as(SMTP_LOGIN, SMTP_PWD)
    .startTtls(true)()

  private val SENDER = "a.cherici@gmail.com"
  private val SUBJECT = "Registration to DAF"
  private val CONTENT = "Clik on this link to complete registration:\n"

  private val LINK = ConfigReader.registrationUrl


}
