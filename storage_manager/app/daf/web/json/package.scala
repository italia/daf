package daf.web

import play.api.libs.json.{ JsError, JsObject, Reads }

package object json {


  implicit class JsonReadsSyntax[A](reads: Reads[A]) {

    def optional: Reads[Option[A]] = Reads.optionWithNull[A](reads)

  }



}
