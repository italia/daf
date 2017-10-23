package it.gov.daf.ingestion.test

import it.gov.daf.ingestion.NiFiBuilder
import javax.inject.Inject

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.ws._
import play.api.libs.ws.ahc.AhcWSClient

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global


object ProvaLog extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val client: AhcWSClient = AhcWSClient()
  val niFiBuilder = new NiFiBuilder(client)

  val niFiResults = niFiBuilder.processorBuilder()
  println(niFiResults)
}
