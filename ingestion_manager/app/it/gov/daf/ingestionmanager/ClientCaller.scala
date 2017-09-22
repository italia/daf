package it.gov.daf.ingestionmanager

import java.io.File
import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import ingestion_manager.yaml.Successfull
import it.gov.daf.catalogmanager.{Dataset, MetaCatalog, StdUris}
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
    val catalogManager: Catalog_managerClient = new Catalog_managerClient(client)(uriCatalogManager)
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


  def callCatalogManager(auth: String, logicalUri: String): Future[String] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val client: AhcWSClient = AhcWSClient()

    //val catalogManager = new Catalog_managerClient(client)(uriCatalogManager)
    //val service = s"$uriCatalogManager/dataset-catalogs/$uri"
    //val response = ingestionManager.connect(client)(service)

    val logical_uri = URLEncoder.encode(logicalUri)
    val url = uriCatalogManager + "/catalog-manager/v1/catalog-ds/get/" + logical_uri
    //val response = catalogManager.standardsuri(auth)

    client.url(url).withHeaders("Authorization" ->  auth).get().map { response =>
        val metadata = response.json
        println("Qui tutto il json")
        println(metadata)
        val name = ((metadata \ "dcatapit") \ "name").as[String]
        name
       }.andThen { case _ => client.close() }
        .andThen { case _ => system.terminate() }

    }



}
