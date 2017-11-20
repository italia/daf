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

import it.gov.daf.common.authentication.Authentication
import org.apache.commons.net.util.Base64
import play.api.libs.json._
import play.api.mvc.Request

import scala.util.Try


/**
  * Created by ale on 11/05/17.
  */

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.ToString",
    "org.wartremover.warts.TraversableOps"
  )
)
object WebServiceUtil {


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

    def readCredentialFromRequest( request:Request[Any] ) :(Option[String],Option[String]) ={


    val auth = request.headers.get("authorization")
    val authType = auth.get.split(" ")(0)

    if( authType.equalsIgnoreCase("basic") ){

      // LDAP profiles are only created  during BA
      println("profiles:"+Authentication.getProfiles(request))
      val user:Option[String] = Option( Authentication.getProfiles(request).head.getId )
      println("userId:"+user)

      val userAndPass = new String(Base64.decodeBase64(auth.get.split(" ").drop(1).head.getBytes))
        .split(":")
      ( user, Option(userAndPass.drop(1).mkString(":")))

    }else if( authType.equalsIgnoreCase("bearer") ) {
      val user:Option[String] = Option( Authentication.getClaims(request).get.get("sub").get.toString )
      println("JWT user:"+user)
      (user , None)
    }else
      throw new Exception("Authorization header not found")


    //val userAndPass = if (auth.get.contains(" ")) new String(Base64.decodeBase64(auth.get.split(" ").drop(1).head.getBytes)).split(":")
    //                  else new String(Base64.decodeBase64(auth.get.getBytes)).split(":")


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
