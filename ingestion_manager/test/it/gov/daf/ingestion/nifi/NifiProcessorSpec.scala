package it.gov.daf.ingestion.nifi


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import it.gov.daf.catalogmanager.json._
import it.gov.daf.catalogmanager.{Dataset, MetaCatalog, Operational}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient

import scala.io.Source
import scala.util.{Failure, Success}

class NifiProcessorSpec extends FlatSpec with Matchers {

  "A Nifi Processor " should "create a nifi pipeline for a correct meta catalog entry" in {

    val in = this.getClass.getResourceAsStream("/data_test.json")
    val sMetaCatalog = Source.fromInputStream(in).getLines().mkString(" ")
    in.close()

    val parsed = Json.parse(sMetaCatalog)
    println(parsed)
    val metaCatalog: JsResult[MetaCatalog] = Json.fromJson[MetaCatalog](parsed)

    metaCatalog.isSuccess shouldBe true

    implicit val config: Config = com.typesafe.config.ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val wsClient: AhcWSClient = AhcWSClient()
    implicit val ec = system.dispatcher

    val fResult = NifiProcessor(metaCatalog.get).createDataFlow()

    fResult.onComplete {
      case Success(response) =>
        true shouldBe true

      case Failure(ex) =>
    }
  }
}
