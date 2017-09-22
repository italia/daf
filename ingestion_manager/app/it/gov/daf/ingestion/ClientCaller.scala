package it.gov.daf.ingestion

import java.io.File
import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import ingestion_manager.yaml.Successfull
import it.gov.daf.ingestion.ClientCaller.ServerRequest
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ale on 14/06/17.
  */

//This object needs to be created once per service called, and renamed accordingly

object ClientCaller {

  import scala.concurrent.ExecutionContext.Implicits.global
  type ServerType = Any
  type ServerRequest = Any

  val uriCatalogManager = ConfigFactory.load().getString("WebServices.catalogManagerUrl")


  def callSrv(auth: String, param :String) : Future[String] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val client: AhcWSClient = AhcWSClient()

    //to modify with the correct client class that has been defined in build as dependency
    //val serviceClient = new Service_managerClient(client)(uriSrvManager)

    //Anytime the param is a uri, you need to trun this
    //val logical_uri = URLEncoder.encode(logicalUri)

    /*
    val response: Future[ServerRequest] = serviceClient.get("param")
    val res: Future[String] = response
      .map{
        case Success(a) => "Operation OK"
        //case Success(false) => Successfull(Some("ERROR"))
        case Failure(ex) => s"ERROR ${ex.getStackTrace.mkString("\t\n")}"
      }

    response.value
    */
    println(uriCatalogManager)
    val logical_uri = URLEncoder.encode(param)
    val url = uriCatalogManager + "/catalog-manager/v1/catalog-ds/get/" + logical_uri
    //val response = catalogManager.standardsuri(auth)
    println(url)

    val test = client.url(url).withHeaders(("Authorization" ->  auth), ("user"->"admin"), ("password"->"admin") ).get()

    println(test)

    test.map { response =>
      val metadata = response.json
      println("Qui tutto il json")
      println(metadata)
      val name = ((metadata \ "dcatapit") \ "name").as[String]
      name
    }.andThen { case _ => client.close() }
      .andThen { case _ => system.terminate() }

  }


}

class Service_managerClient(client: AhcWSClient)(uriSrvManager: String) {

  def get(param: String): Future[ServerRequest] = {
    Future(param)
  }
}
