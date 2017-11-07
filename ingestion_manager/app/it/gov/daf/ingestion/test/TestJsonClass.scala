package it.gov.daf.ingestion.test

import play.api.libs.json._
import it.gov.daf.catalogmanager.json._
import it.gov.daf.catalogmanager.{InputSrc, StorageHdfs, SourceSftp}
import org.apache.commons.lang.StringEscapeUtils

/*
@SuppressWarnings(
Array(
“org.wartremover.warts.Throw”,
“org.wartremover.warts.ToString”,
“org.wartremover.warts.Null”
)
)
*/

object TestJsonClass extends App{

/*
  final case class CiccioName(name: String, cognome:String)
  final case class Ciccio(name: Option[List[CiccioName]], name2: Option[List[String]], prova: String)

  val ciccio = Ciccio(Some(List(CiccioName("nome", "cognome"), CiccioName("nome2", "cognome2"))), None, "ciccio")

  implicit val ciccioNameWrites: Writes[CiccioName] = new Writes[CiccioName] {
    def writes(ciccioName: CiccioName) = JsObject(Seq(
      "name" -> Json.toJson(ciccioName.name),
      "cognome" -> Json.toJson(ciccioName.cognome)
    ).filter(_._2 != JsNull))
  }

  implicit val ciccioWrites: Writes[Ciccio] = new Writes[Ciccio] {
    def writes(ciccio: Ciccio) = JsObject(Seq(
      "name" -> Json.toJson(ciccio.name),
      "name2" -> Json.toJson(ciccio.name2),
      "prova" -> Json.toJson(ciccio.prova)
    ).filter(_._2 != JsNull))
  }
  */

val ciccio = InputSrc(sftp=Some(List(SourceSftp("sftp_daf", None, None, None, Some("param")))),
  srv_pull = None, srv_push = None, daf_dataset = None)

  println(Json.toJson(ciccio).toString())
  println(StringEscapeUtils.escapeJava(Json.toJson(ciccio).toString))
}
