package it.gov.daf.catalogmanager.tempo

import java.util.concurrent.TimeUnit
import javax.mail.internet.InternetAddress

import courier.{Envelope, Mailer, Text}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


class MailService(to:String,token:String) {

  import scala.concurrent.ExecutionContext.Implicits._

  def sendMail():Future[String]={

   // def fut =
   def fut = MailService.mailer(Envelope.from(new InternetAddress(MailService.SENDER))
    .to(new InternetAddress(to))
    //.cc(new InternetAddress("dad@gmail.com") )
    .subject(MailService.SUBJECT)
    .content(Text(MailService.CONTENT+MailService.LINK+token)))

    fut map { _ =>
      "Mail sent"
    }

    //println("we")
    /*
    fut.onComplete {
      case _ => println("completed")
    }*/

    //Await.ready(fut, Duration(60, TimeUnit.SECONDS))

  }

}


object MailService{

  private val SMTP_SERVER = "smtp.gmail.com"
  private val SMTP_PORT = 587
  private val SMTP_LOGIN = "maildaf2017@gmail.com"
  private val SMTP_PWD = "birradaf"

  private val mailer = Mailer(SMTP_SERVER, SMTP_PORT)
    .auth(true)
    .as(SMTP_LOGIN, SMTP_PWD)
    .startTtls(true)()

  private val SENDER = "a.cherici@gmail.com"
  private val SUBJECT = "Registration to DAF"
  private val CONTENT = "Clik on this link to complete registration:\n"
  private val LINK = "http://localhost:9001/bo?t="

}
