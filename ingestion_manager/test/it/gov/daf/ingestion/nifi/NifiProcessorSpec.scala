package it.gov.daf.ingestion.nifi


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.catalogmanager.json._
import it.gov.daf.ingestion.metacatalog.MetaCatalogProcessor
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future
import scala.io.Source

class NifiProcessorSpec extends AsyncFlatSpec with Matchers {

  "A Nifi Processor " should "create a nifi pipeline for a correct meta catalog entry" in {

    val in = this.getClass.getResourceAsStream("/data_test.json")
    val sMetaCatalog = Source.fromInputStream(in).getLines().mkString(" ")
    in.close()

    val parsed = Json.parse(sMetaCatalog)
    val metaCatalog: JsResult[MetaCatalog] = Json.fromJson[MetaCatalog](parsed)

    metaCatalog.isSuccess shouldBe true

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val wsClient: AhcWSClient = AhcWSClient()

    implicit val config: Config = com.typesafe.config.ConfigFactory.load()
    implicit val ec = system.dispatcher

    def closeAll(): Unit = {
      system.terminate()
      materializer.shutdown()
      wsClient.close()
    }

    val fResult = NifiProcessor(metaCatalog.get).createDataFlow()

    fResult.map { response =>
      println(response)
      closeAll()
      true shouldBe true
    }
  }


}