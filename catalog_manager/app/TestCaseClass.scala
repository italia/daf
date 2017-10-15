import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import de.zalando.play.controllers.WriterFactories
import it.gov.daf.catalogmanager.{ConversionField, MetaCatalog, StdSchema}
import play.api.libs.json._

object TestCaseClass extends App{

  //@JsonInclude(JsonInclude.Include.NON_ABSENT)
  case class Pippo (ogg1: Option[String], ogg2: String, ogg3: Option[Pluto])
  //case class Pippo (ogg1: Option[String], ogg2: String)
  //case class Pluto (prop1: String, prop2: String, prop3: List[String])
  case class Pluto (prop1: String, prop2: String)

  val pippo = Pippo(Some("ciao"), "", None)
  //val pippo = Pippo(Some("ciao"), "ciao2")

  def jacksonMapper(mimeType: String): ObjectMapper = {
    //noinspection ScalaStyle
    assert(mimeType != null)
    val factory = WriterFactories.factories(mimeType)
    val mapper = new ObjectMapper(factory)
    mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
    mapper.registerModule(DefaultScalaModule)

    mapper
  }
  val json = jacksonMapper("blabla").writeValueAsString(pippo)
  //val json = "{\"ogg1\":\"ciao\",\"ogg2\":\"ciao2\"}"
  println(json)
  //implicit val plutoReads = Json.reads[Pluto]
  //implicit val pippoReads = Json.reads[Pippo]
  //implicit val plutoReads = Json.reads[Option[Pluto]]
  /*
  implicit lazy val PlutoReads: Reads[Pluto] = Reads[Pluto] {
    json => JsSuccess(Pluto((json \ "prop1").as[String], (json \ "prop2").as[String], (json \ "prop3").as[List[String]]))
  }
  */

  implicit lazy val PlutoReads: Reads[Pluto] = Reads[Pluto] {
    json => JsSuccess(Pluto((json \ "prop1").as[String], (json \ "prop2").as[String]))
  }
  implicit lazy val PippoReads: Reads[Pippo] = Reads[Pippo] {
    json => JsSuccess(Pippo((json \ "ogg1").asOpt[String], (json \ "ogg2").as[String], (json \ "ogg3").asOpt[Pluto]))
  }

  val res = Json.parse(json).as[Pippo]
  println(res)

  /*
  val jsonParsed = Json.parse(json)
  val residentFromJson: JsResult[Pippo] = Json.fromJson[Pippo](jsonParsed)

  residentFromJson match {
    case JsSuccess(r: Pippo, path: JsPath) => println("Name: " + r.ogg1)
    case e: JsError => println("Errors: " + JsError.toJson(e).toString())
  }
*/




}
