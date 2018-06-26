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

package daf.web.json

import play.api.libs.json.{ JsError, JsObject, Reads }

object CommonReads {

  def choice[A](branches: PartialFunction[String, Reads[A]]): Reads[A] = Reads {
    case js @ JsObject(obj) => obj.find { case (key, _) => branches.isDefinedAt(key) }.map { case (key, _) => branches(key).reads(js) } getOrElse JsError { "No matching path in object" }
    case _                  => JsError { "Cannot apply choice to non-object types" }
  }

}
