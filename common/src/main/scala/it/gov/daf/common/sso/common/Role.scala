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

package it.gov.daf.common.sso.common


sealed abstract class Role(name : String){
  override def toString = name
}

case object SysAdmin extends Role("daf_sys_admin")
case object Admin extends Role("daf_adm_")
case object Editor extends Role("daf_edt_")
case object Viewer extends Role("daf_vwr_")

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements"
  )
)
object Role{

  val rolesPrefixs:Seq[String] = Seq(SysAdmin.toString, Admin.toString, Editor.toString, Viewer.toString)

  def pickRole( lista:Array[String],group:String ):Option[Role] = {

    val appo:Array[String] = lista.filter(_.endsWith(group))

    if( appo.contains(Admin.toString+group) )
      Some(Admin)
    else if( appo.contains(Editor.toString+group) )
      Some(Editor)
    else if( appo.contains(Viewer.toString+group) )
      Some(Viewer)
    else
      None
  }
}




