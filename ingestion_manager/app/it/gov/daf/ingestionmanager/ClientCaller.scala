package it.gov.daf.ingestionmanager

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

/**
  * Created by ale on 14/06/17.
  */
object ClientCaller {

  import scala.concurrent.ExecutionContext.Implicits.global

  val ingestionManager = new IngestionManager()
  val uriCatalogManager = ConfigFactory.load().getString("WebServices.catalogUrl")

  def callCatalogManager(logicalUri :String, upfile :File ) : Future[Successfull] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val client: AhcWSClient = AhcWSClient()
    val catalogManager = new Catalog_managerClient(client)(uriCatalogManager)
    //val service = s"$uriCatalogManager/dataset-catalogs/$uri"
    //val response = ingestionManager.connect(client)(service)
    val logical_uri = URLEncoder.encode(logicalUri)
    val response: Future[MetaCatalog] = catalogManager.datasetcatalogbyid("",logical_uri)
    val res: Future[Successfull] = response
      .map(s =>  ingestionManager.write(s, upfile))
      .map{
        case Success(true) => Successfull(Some("Dataset stored"))
        case Success(false) => Successfull(Some("ERROR dataset cannot be stored"))
        case Failure(ex) => Successfull(Some(s"ERROR ${ex.getStackTrace.mkString("\t\n")}"))
      }
    res
  }

}
