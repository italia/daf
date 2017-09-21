package it.gov.daf.microsrv

import java.io.File
import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import ingestion_manager.yaml.Successfull
import it.gov.daf.microsrv.ClientCaller.ServerRequest
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by ale on 14/06/17.
  */

//This object needs to be created once per service called, and renamed accordingly

object ClientCaller {

  import scala.concurrent.ExecutionContext.Implicits.global
  type ServerType = Any
  type ServerRequest = Any

  val uriSrvManager = ConfigFactory.load().getString("WebServices.servManagerUrl")


  def callSrv(param :String) : Future[Successfull] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val client: AhcWSClient = AhcWSClient()

    //to modify with the correct client class that has been defined in build as dependency
    val serviceClient = new Service_managerClient(client)(uriSrvManager)

    //Anytime the param is a uri, you need to trun this
    //val logical_uri = URLEncoder.encode(logicalUri)


    val response: Future[ServerRequest] = serviceClient.get("param")
    val res: Future[Successfull] = response
      .map{
        case Success(true) => Successfull(Some("Dataset stored"))
        case Success(false) => Successfull(Some("ERROR dataset cannot be stored"))
        case Failure(ex) => Successfull(Some(s"ERROR ${ex.getStackTrace.mkString("\t\n")}"))
      }
    res
  }

}

class Service_managerClient(client: AhcWSClient)(uriSrvManager: String) {

  def get(param: String): Future[ServerRequest] = {
    Future(param)
  }
}
