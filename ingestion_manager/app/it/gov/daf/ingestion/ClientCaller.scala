package it.gov.daf.ingestion

import java.net.URLEncoder
import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

/**
  * Created by ale on 14/06/17.
  */

//This object needs to be created once per service called, and renamed accordingly

class ClientCaller(private val wsClient: AhcWSClient)
                  (implicit actorSystem: ActorSystem, mat: ActorMaterializer) {

  type ServerType = Any
  type ServerRequest = Any

  private val uriCatalogManager = ConfigFactory.load().getString("WebServices.catalogManagerUrl")

  //this should be created only one time
  private val catalogManagerClient = new Catalog_managerClient(wsClient)(uriCatalogManager)

  def clientCatalogMgrMetaCatalog(auth: String, logicalUri: String): Future[MetaCatalog] = {
    //to modify with the correct wsClient class that has been defined in build as dependency
    //val serviceClient = new Service_managerClient(wsClient)(uriSrvManager)

    //Anytime the param is a uri, you need to trun this
    val logicalUriEncoded = URLEncoder.encode(logicalUri, "UTF-8")
    catalogManagerClient.datasetcatalogbyid(auth, logicalUriEncoded)
  }

}

