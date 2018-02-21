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

import java.net.URLEncoder
import play.api.libs.json._


/**
  * Created by ale on 11/05/17.
  */

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.ToString"
  )
)
object  WebServiceUtil {

  def buildEncodedQueryString(params: Map[String, Any]): String = {
    val encoded = for {
      (name, value) <- params if value != None
      encodedValue = value match {
        case Some(x)         => URLEncoder.encode(x.toString, "UTF8")
        case x               => URLEncoder.encode(x.toString, "UTF8")
      }
    } yield name + "=" + encodedValue

    encoded.mkString("?", "&", "")
  }




  def cleanDquote(in:String): String = {
    in.replace("\"","").replace("[","")replace("]","")
  }

  def getMessageFromJsError(error:JsError): String ={

    val jsonError = JsError.toJson(error)

    if( (jsonError \ "obj").toOption.isEmpty )
      jsonError.value.foldLeft("ERRORS--> "){ (s: String, pair: (String, JsValue)) =>
        s + "field: "+pair._1 +" message:"+ (pair._2 \\ "msg")(0).toString + "  "
      }
    else
      cleanDquote( (( (jsonError \ "obj")(0) \ "msg").getOrElse(JsArray(Seq(JsString(" ?? "))))(0) ).get.toString() )

  }


  def getMessageFromCkanError(error:JsValue): String ={


    val errorMsg = (error \ "error").getOrElse(JsString("can't retrive error") )

    val ckanError = errorMsg.as[JsObject].value.foldLeft("ERRORS: "){ (s: String, pair: (String, JsValue)) =>
      s + "<< field: "+pair._1 +"  message: "+ cleanDquote(pair._2.toString()) + " >>   "}

    ckanError

  }

}
