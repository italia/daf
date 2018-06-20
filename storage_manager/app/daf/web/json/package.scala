package daf.web

import play.api.libs.json._

package object json {


  implicit class JsonReadsSyntax[A](reads: Reads[A]) {

    private def downAt(at: String)(jsValue: JsValue) = (__ \ at).asSingleJson(jsValue) match {
      case JsDefined(_)  => Reads.optionWithNull(reads).reads(jsValue)
      case JsUndefined() => JsSuccess { None }
    }

    /**
      * Checks whether a JsPath exists before it applies [[reads]].
      * @param at the node in the JsValue that should exist
      * @return an `Reads` instance that will read only if the path exists and is not `JsNull`
      */
    def optional(at: String): Reads[Option[A]] = Reads { downAt(at) }

  }



}
