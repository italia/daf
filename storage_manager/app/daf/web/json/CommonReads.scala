package daf.web.json

import play.api.libs.json.{ JsError, JsObject, Reads }

object CommonReads {

  def choice[A](branches: PartialFunction[String, Reads[A]]): Reads[A] = Reads {
    case js @ JsObject(obj) => obj.find { case (key, _) => branches.isDefinedAt(key) }.map { case (key, _) => branches(key).reads(js) } getOrElse JsError { "No matching path in object" }
    case _                  => JsError { "Cannot apply choice to non-object types" }
  }

}
