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
