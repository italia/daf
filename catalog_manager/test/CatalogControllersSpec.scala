

import java.io.IOException
import java.net.ServerSocket

import catalog_manager.yaml.MetaCatalog
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.Status
import play.api.routing.Router
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test._
import play.api.test.Helpers._

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

    "Call catalog-manager/v1/dataset-catalogs return a non empty list" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url(s"http://localhost:9000/catalog-manager/v1/dataset-catalogs").
            execute, Duration.Inf)
          println(response.status)
          println("ALE")
          println(response.body)
          val json = Json.parse(response.body)
          json.as[List[MetaCatalog]] must not be empty
        }
      }

    "Call catalog-manager/v1/dataset-catalogs/ return ok status" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url(s"http://localhost:9000/catalog-manager/v1/dataset-catalogs").
            execute, Duration.Inf)
          println(response.status)
          response.status must be equalTo Status.OK
        }
      }
  }
}
