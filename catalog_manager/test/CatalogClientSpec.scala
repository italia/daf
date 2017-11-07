import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by ale on 09/06/17.
  */
class CatalogClientSpec extends Specification  {

  def application: Application = GuiceApplicationBuilder().build()

  "The catalog-client" should {
    "call catalog-manager/v1/dataset-catalogs return a non empty list" in
      new WithServer(app = application, port = 9000) {
        implicit val system: ActorSystem = ActorSystem()
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        val clientManager: AhcWSClient = AhcWSClient()
        val catalogManager = new Catalog_managerClient(clientManager)("http://localhost:9001")
        //val test: List[it.gov.daf.catalogmanager.MetaCatalog] = Await.result(catalogManager.datasetcatalogs("test:test"), Duration.Inf)
        //test.size must be greaterThan(0)
      }

    /*
    "call catalog-manager/v1/dataset-catalogs and all catalogs must have logical_uri" in
      new WithServer(app = application, port = 9000) {
        implicit val system: ActorSystem = ActorSystem()
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        val clientManager: AhcWSClient = AhcWSClient()
        val catalogManager = new Catalog_managerClient(clientManager)("http://localhost:9001")
        val test: List[it.gov.daf.catalogmanager.MetaCatalog] = Await.result(catalogManager.datasetcatalogs("test:test"), Duration.Inf)
        val checker: Seq[Boolean] = test.map(x => x.operational.logical_uri match {
          case Some(_) => true
          case None => false
        })
        val andAll: Boolean = checker.foldLeft(true)(_ && _)
        andAll must be equals  true
      }
    */

  }

}

