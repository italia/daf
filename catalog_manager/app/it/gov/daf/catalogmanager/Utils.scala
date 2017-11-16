package it.gov.daf.catalogmanager

import java.net.URLEncoder

import play.api.libs.json._

object Utils {

  def getMessageFromJsError(error: JsError): String = {
    val jsonError = JsError.toJson(error)

    if ((jsonError \ "obj").toOption.isEmpty) {
      jsonError.value.foldLeft("ERRORS--> ") { (s: String, pair: (String, JsValue)) =>
        s + "field: " + pair._1 + " message:" + (pair._2 \\ "msg")(0).toString + "  "
      }
    } else cleanDquote(((jsonError \ "obj")(0) \ "msg").getOrElse(JsArray(Seq(JsString(" ?? "))))(0).get.toString())
  }

  def cleanDquote(in: String): String = {
    in.replace("\"", "").replace("[", "") replace ("]", "")
  }

  def getMessageFromCkanError(error: JsValue): String = {
    val errorMsg = (error \ "error").getOrElse(JsString("can't retrive error"))

    val ckanError = errorMsg.as[JsObject].value.foldLeft("ERRORS: ") { (s: String, pair: (String, JsValue)) =>
      s + "<< field: " + pair._1 + "  message: " + cleanDquote(pair._2.toString()) + " >>   "
    }
    ckanError
  }

  def buildEncodedQueryString(params: Map[String, Any]): String = {
    val encoded = for {
      (name, value) <- params if value != None
      encodedValue = value match {
        case Some(x) => URLEncoder.encode(x.toString, "UTF8")
        case x => URLEncoder.encode(x.toString, "UTF8")
      }
    } yield name + "=" + encodedValue

    encoded.mkString("?", "&", "")
  }
}
