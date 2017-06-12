

import java.io.IOException
import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import catalog_manager.yaml.MetaCatalog
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.Status
import play.api.routing.Router
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test._
import it.gov.daf.catalogmanager
import it.gov.daf.catalogmanager.client.Catalog_managerClient

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by ale on 08/06/17.
  */
class CatalogControllersSpec extends Specification  {

  def application: Application = GuiceApplicationBuilder().build()

  import catalog_manager.yaml.BodyReads.MetaCatalogReads

  "The catalog-manager" should {
    "Call catalog-manager/v1/dataset-catalogs return ok status" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url(s"http://localhost:9000/catalog-manager/v1/dataset-catalogs").
            execute, Duration.Inf)
          println(response.status)
          response.status must be equalTo Status.OK
        }
      }

    "Call catalog-manager/v1/dataset-catalogs return a non empty list if" +
      "you have error maybe is necessaty to add data to db" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url(s"http://localhost:9000/catalog-manager/v1/dataset-catalogs").
            execute, Duration.Inf)
          println(response.status)
          println("ALE")
          println(response.body)
          val json: JsValue = Json.parse(response.body)
          json.as[JsArray].value.size must be greaterThan (0)
        }
      }


    "The catalog-manager" should {
      "Call catalog-manager/v1/dataset-catalogs/{logical_uri} return ok status" in
        new WithServer(app = application, port = 9000) {
          val logicalUri = "daf://dataset/std/standard/standard/uri_cultura/standard"
          val url = s"http://localhost:9000/catalog-manager/v1/dataset-catalogs/$logicalUri"
          println(url)
          WsTestClient.withClient { implicit client =>
            val response: WSResponse = Await.result[WSResponse](client.
              url(url).
              execute, Duration.Inf)
            println(response.status)
            response.status must be equalTo Status.OK
          }
        }
    }

    "The catalog-manager" should {
      "Call catalog-manager/v1/dataset-catalogs/{anything} return 401" in
        new WithServer(app = application, port = 9000) {
          val logicalUri = "anything"
          val url = s"http://localhost:9000/catalog-manager/v1/dataset-catalogs/$logicalUri"
          println(url)
          WsTestClient.withClient { implicit client =>
            val response: WSResponse = Await.result[WSResponse](client.
              url(url).
              execute, Duration.Inf)
            println(response.status)
            response.status must be equalTo 401
          }
        }
    }
  }
}
