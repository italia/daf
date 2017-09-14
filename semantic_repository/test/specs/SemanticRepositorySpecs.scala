package specs

import org.junit.runner.RunWith

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

import play.api.test._
import play.api.http.Status
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import it.almawave.linkeddata.kb.utils.ConfigHelper

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import play.twirl.api.Content
import play.api.test.Helpers._
import play.api.libs.json.JsObject
import java.io.File
import play.api.http.Writeable
import akka.stream.scaladsl.Source
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile
import java.nio.file.Files
import org.asynchttpclient.AsyncHttpClient
import play.api.libs.ws.WS
import akka.util.ByteString
import play.api.mvc.MultipartFormData.DataPart
import play.api.mvc.MultipartFormData.FilePart
import akka.stream.scaladsl.FileIO
import play.api.libs.ws.WSClient

@RunWith(classOf[JUnitRunner])
class SemanticRepositorySpecs extends Specification {

  def application: Application = GuiceApplicationBuilder().build()

  "The lod-manager" should {

    "call kb/v1/contexts to obtain a list of contexts" in {
      new WithServer(app = application, port = 9999) {
        WsTestClient.withClient { implicit client =>

          val response: WSResponse = Await.result[WSResponse](
            client.url(s"http://localhost:${port}/kb/v1/contexts").execute,
            Duration.Inf)

          response.status must be equalTo Status.OK
          response.json.as[Seq[JsObject]].size must be equals 0

        }
      }
    }

    "call kb/v1/contexts ensuring all contexts have triples" in {
      new WithServer(app = application, port = 9999) {
        WsTestClient.withClient { implicit client =>

          val response: WSResponse = Await.result[WSResponse](
            client.url(s"http://localhost:${port}/kb/v1/contexts").execute,
            Duration.Inf)

          val json_list = response.json.as[Seq[JsObject]]
          forall(json_list)((_) must not beNull)
          forall(json_list)(_.keys must contain("context", "triples"))
          forall(json_list)(item => (item \ "triples").get.as[Int] > 0)

        }
      }
    }

  }

}