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

package it.gov.daf.common.authentication

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Product",
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.Serializable"
  )
)
object Role {

  sealed abstract class EnumVal(name : String){
    override def toString = name
  }

  case object Admin extends EnumVal("daf_admins")
  case object Editor extends EnumVal("daf_editors")
  case object Viewer extends EnumVal("daf_viewers")

  val roles = Seq(Admin, Editor, Viewer)

}
