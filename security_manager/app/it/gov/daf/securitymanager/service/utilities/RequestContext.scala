package it.gov.daf.securitymanager.service.utilities

import java.text.SimpleDateFormat
import java.util.Calendar

import it.gov.daf.common.sso.common.CredentialManager
import org.slf4j.MDC
import play.api.mvc.RequestHeader

import scala.util.Success

object RequestContext {

  def execInContext[S](contextName:String)(fx:()=>S)(implicit requestHeader: RequestHeader):S ={

    /*
    println("Thread: >>>> "+Thread.currentThread().getId())
    println("Thread name: >>>> "+Thread.currentThread().getName())
    println("ClassLoader: >>>> "+Thread.currentThread().getContextClassLoader().hashCode())
    println("MDC: >>>> "+MDC.getMDCAdapter)
    println("CM: >>>> "+MDC.getMDCAdapter.getCopyOfContextMap)
    println(">>>>>>>>>>>>>>>>>>>>>PUT")*/

    try{

      MDC.put("context-name", contextName)

      val now = Calendar.getInstance().getTime()
      val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
      val currentDate = format.format(now)
      MDC.put("request-init-time", currentDate)

      val username = CredentialManager.tryToReadCredentialFromRequest(requestHeader) match{
       case Success(ui) => ui.username
       case _ => "anonymous"
      }

      MDC.put("user-id",username)

      fx()

    }finally{
      MDC.clear()
    }

  }

  def getUsername():String = MDC.get("user-id")

}
