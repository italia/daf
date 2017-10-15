package it.gov.daf.ingestion

import java.io.File
import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import ingestion_manager.yaml.Successfull
import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.catalogmanager.client.Catalog_managerClient
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


  def clientCatalogMgrMetaCatalog(auth: String, logicalUri :String) : Future[MetaCatalog] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val client: AhcWSClient = AhcWSClient()

    //to modify with the correct client class that has been defined in build as dependency
    //val serviceClient = new Service_managerClient(client)(uriSrvManager)

    //Anytime the param is a uri, you need to trun this
    val logicalUriEncoded = URLEncoder.encode(logicalUri)

    val catalogManagerClient = new Catalog_managerClient(client)(uriCatalogManager)
    val response: Future[MetaCatalog] = catalogManagerClient.datasetcatalogbyid(auth,logicalUriEncoded)

    response

  }


}

