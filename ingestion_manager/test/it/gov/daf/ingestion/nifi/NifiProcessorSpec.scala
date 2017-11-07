package it.gov.daf.ingestion.nifi

import it.gov.daf.catalogmanager.json._
import it.gov.daf.catalogmanager.{Dataset, MetaCatalog, Operational}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._

import scala.io.Source

class NifiProcessorSpec extends FlatSpec with Matchers {

  "A Nifi Processor " should "create a nifi pipeline for a correct meta catalog entry" in {

    val in = this.getClass.getResourceAsStream("/data_test.json")
    val sMetaCatalog = Source.fromInputStream(in).getLines().mkString(" ")
    in.close()

    val parsed = Json.parse(sMetaCatalog)
    println(parsed)
    val metaCatalog: JsResult[MetaCatalog] = Json.fromJson[MetaCatalog](parsed)

    println(metaCatalog)

  }
}
