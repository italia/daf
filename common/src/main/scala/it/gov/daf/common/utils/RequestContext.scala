/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.gov.daf.common.utils

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
