package specs
//
//import java.io.IOException
//import java.net.ServerSocket
//
//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer

//import play.api.routing.Router
//import play.api.libs.json.{ JsArray, JsValue, Json }

//
//
//import lod_manager.yaml.Error
//import org.junit.runner.RunWith
//
//import org.scalatest.Specs
//import org.specs2.runner.JUnitRunner
//import org.specs2.runner.JUnitRunner

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
//import org.specs2.Specification
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import it.almawave.linkeddata.kb.utils.ConfigHelper

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
//import play.api.libs.json.JsObject
import play.twirl.api.Content
import play.api.test.Helpers._
import play.api.libs.json.JsObject
import java.io.File
import play.api.http.Writeable
import akka.stream.scaladsl.Source
//import play.mvc.Http$MultipartFormData.Part
//import play.api.mvc.MultipartFormData.FilePart
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

//import org.asynchttpclient.request.body.multipart.StringPart
//import org.asynchttpclient.request.body.multipart.FilePart

@RunWith(classOf[JUnitRunner])
class LODManagerSpec extends Specification {

  def application: Application = GuiceApplicationBuilder().build()

  "The lod-manager" should {

    "call kb/v1/contexts to obtain a list of contexts" in {
      new WithServer(app = application, port = 9999) {
        WsTestClient.withClient { implicit client =>

          val response: WSResponse = Await.result[WSResponse](
            client.url(s"http://localhost:${port}/kb/v1/contexts").execute,
            Duration.Inf)

          response.status must be equalTo Status.OK
          response.json.as[Seq[JsObject]].size must be > 0

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

    "ontonethub: list all the ontologies" in {

      WsTestClient.withClient { implicit client =>

        val response: WSResponse = Await.result[WSResponse](
          client.url(s"http://localhost:8000/stanbol/ontonethub/ontologies")
            .get(),
          Duration.Inf)

        println("\n\n\n\nCHECK: ontonethub - list of ontologies")
        println(response.json)

        action(response)
      }
    }

    //    "add rdf document" in {
    //      new WithServer(app = application, port = 9999) {
    //        WsTestClient.withClient { implicit client =>
    //
    //          //          implicit val wrt: Writeable[Source[_ <: Part[Any], Any]] = null
    //
    //          // ESEMPIO DANIELE
    //          //          val file = Environment.simple().getFile("test/specs/test.ttl").toPath()
    //          //          val fp = FilePart("rdfDocument", "test.ttl", Option("text/turtle"), FileIO.fromPath(file))
    //          //          val dp = List(DataPart("name", "test.ttl"), DataPart("validator", "1"))
    //          //          val source = Source(fp :: dp)
    //          //          val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:${port}/validator/validate").post(source), Duration.Inf)
    //
    //          val body: Source[MultipartFormData.Part[Source[ByteString, _]], _] =
    //            Source(
    //              List(
    //                DataPart("uno", "11"), DataPart("due", "22"),
    //                FilePart("", "", Option("text/turtle"), FileIO.fromFile(new File("ok")))))
    //
    //          val response: WSResponse = Await.result[WSResponse](
    //            client.url(s"http://localhost:${port}/vb/v1/ontologies")
    //              //            .def post(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Future[WSResponse]
    //              .post(body),
    //            //              .withMethod("POST")
    //            //               .withBody(new File(""))
    //            // .withQueryString(params: _*)
    //            //              .execute(),
    //            Duration.Inf)
    //
    //          action(response)
    //
    //        }
    //      }
    //    }

    "uploadFile returns (Missing file)" in new WithApplication {

      //      val asyncHttpClient: AsyncHttpClient = WS.client.underlying
      //      val postBuilder = asyncHttpClient.preparePost("")
      //      val builder = postBuilder
      //        .addBodyPart(new StringPart("myField", "abc", "UTF-8"))
      //        .addBodyPart(new FilePart("myFile", new File("")))
      //        
      //      val response = asyncHttpClient.executeRequest(builder.build()).get()
      //      ok(response.getResponseBody)

    }

  }

}