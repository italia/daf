package it.gov.daf.ingestion.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient


object ProvaLog extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val client: AhcWSClient = AhcWSClient()
  //val niFiBuilder = new NiFiBuilder(client)

  //val niFiResults = niFiBuilder.processorBuilder()
  //println(niFiResults)
}
