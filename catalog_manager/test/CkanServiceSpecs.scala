import java.io.FileInputStream

import org.specs2.mutable.Specification
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.{WithServer, WsTestClient}
import play.api.{Application, Environment}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by andrea on 05/07/17.
  */
class CkanServiceSpecs extends Specification{

  def application: Application = GuiceApplicationBuilder().build()

  private def readJsonFile(path: String):JsValue = {
    val streamDataset = new FileInputStream(Environment.simple().getFile(path))
    try {
      Json.parse(streamDataset)
    }catch {
      case tr: Throwable => tr.printStackTrace(); JsString("Empty file")
    }finally {
      streamDataset.close()
    }
  }

  "The frontend-server" should {

    "Call /ckan/datasets return ok status" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url("http://localhost:9001/ckan/datasets").
            get, Duration.Inf)
          //println(response.status)
          response.status must be equalTo Status.OK
        }
      }

    "Call ckan/createOrganization respond success" in
      new WithServer(app = application, port = 9000) {
        val json =
          WsTestClient.withClient { implicit client =>
            val response: WSResponse = Await.result[WSResponse](client.
              url("http://localhost:9001/ckan/createOrganization").post(readJsonFile("test/org.json")), Duration.Inf)
            //println(response.body)
            response.body must contain(""""success":true""")
          }
      }

    "Call ckan/createDataset respond success" in
      new WithServer(app = application, port = 9000) {
        val json =
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url("http://localhost:9001/ckan/createDataset").post(readJsonFile("test/data.json")), Duration.Inf)
          //println(response.body)
          response.body must contain(""""success":true""")
        }
      }

    "Call ckan/dataset respond success" in
      new WithServer(app = application, port = 9000) {
        val json =
          WsTestClient.withClient { implicit client =>
            val response: WSResponse = Await.result[WSResponse](client.
              url("http://localhost:9001/ckan/dataset/test-dcatapit-api-prova").get, Duration.Inf)
            //println(response.body)
            response.body must contain(""""success":true""")
          }
      }

    "Call ckan/deleteDataset respond success" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url("http://localhost:9001/ckan/deleteDataset/test-dcatapit-api-prova").delete, Duration.Inf)
          //println(response.body)
          response.body must contain(""""success":true""")
        }
      }

    "Call ckan/purge Dataset respond success" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url("http://localhost:9001/ckan/purgeDataset/test-dcatapit-api-prova").delete, Duration.Inf)
          //println(response.body)
          response.body must contain(""""success":true""")
        }
      }

    "Call ckan/deleteOrganization respond success" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url("http://localhost:9001/ckan/deleteOrganization/test-org").delete, Duration.Inf)
          //println(response.body)
          response.body must contain(""""success":true""")
        }
      }

    "Call ckan/purgeOrganization respond success" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url("http://localhost:9001/ckan/purgeOrganization/test-org").delete, Duration.Inf)
          //println(response.body)
          response.body must contain(""""success":true""")
        }
      }

  }

}
