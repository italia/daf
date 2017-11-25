package it.gov.daf.securitymanager.service

object Role {

  sealed abstract class EnumVal(name : String){
    override def toString = name
  }

  case object SysAdmin extends EnumVal("SysAdmin")
  case object Admin extends EnumVal("Admin")
  case object Editor extends EnumVal("Editor")
  case object Viewer extends EnumVal("Viewer")

  val roles = Seq(SysAdmin, Admin, Editor, Viewer)

}
