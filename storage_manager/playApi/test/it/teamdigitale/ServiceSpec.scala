/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.teamdigitale

/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.{IOException, File => JFile}
import java.net.ServerSocket

import it.teamdigitale.DataFrameClasses.{Address, Person}
import play.Logger
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSResponse}
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Throw"))
class ServiceSpec extends HDFSbase {

  val urlParquet = super.pathParquet
  val urlAvro = super.pathAvro
  val urlCSV = super.pathCsv

  def getAvailablePort: Int = {
    try {
      val socket = new ServerSocket(0)
      try {
        socket.getLocalPort
      } finally {
        socket.close()
      }
    } catch {
      case e: IOException =>
        throw new IllegalStateException(s"Cannot find available port: ${e.getMessage}", e)
    }
  }

  private def application: Application = GuiceApplicationBuilder().
    configure("pac4j.authenticator" -> "test").
    build()

  private def getPeople(string: String): Option[List[Person]] = {
    implicit val addressFormat = Json.format[Address]
    implicit val personFormat = Json.format[Person]

    Json.parse(string).asOpt[List[Person]]
  }

  "The storage_manager" should "manage the physical datasets correctly" in new WithServer(app = application, port = getAvailablePort) {

    WsTestClient.withClient { implicit client =>
      val params = Map("protocol" -> "hdfs", "path" -> urlParquet)
      val json = Json.toJson(params)
      Logger.info(s"$json")
      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets")
        .post(json), Duration.Inf)
      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.UNAUTHORIZED
    }

    //Test with a wrong dataset URI
    WsTestClient.withClient { implicit client =>
      val wrongUrl = urlParquet.replace(".", "")
      val params = Map("protocol" -> "hdfs", "path" -> wrongUrl)
      val json = Json.toJson(params)
      Logger.info(s"$json")
      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets").
        withAuth("david", "david", WSAuthScheme.BASIC).
        post(json), Duration.Inf)
      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.BAD_REQUEST
    }

    //Test with a wrong dataset scheme
    WsTestClient.withClient { implicit client =>
      val params = Map("protocol" -> "WRONG", "path" -> urlParquet)
      val json = Json.toJson(params)
      Logger.info(s"$json")
      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets").
        withAuth("david", "david", WSAuthScheme.BASIC).
        post(json), Duration.Inf)
      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.BAD_REQUEST
    }

    WsTestClient.withClient { implicit client =>

      val params = Map("protocol" -> "hdfs", "path" -> urlParquet)
      val json = Json.toJson(params)

      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets").
        withAuth("david", "david", WSAuthScheme.BASIC).
        post(json), Duration.Inf)

      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.OK

      val people = getPeople(response.body)

      people should not be empty
      people.get.size should be > 0
    }

    WsTestClient.withClient { implicit client =>

      val params = Map("protocol" -> "hdfs", "path" -> urlAvro, "format" -> "avro")
      val json = Json.toJson(params)

      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets").
        withAuth("david", "david", WSAuthScheme.BASIC).
        post(json), Duration.Inf)

      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.OK

      val people = getPeople(response.body)

      people should not be empty
      people.get.size should be > 0
    }

    WsTestClient.withClient { implicit client =>
      val params = Map("protocol" -> "hdfs", "path" -> urlCSV, "format" -> "csv")
      val json = Json.toJson(params)

      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets").
        withAuth("david", "david", WSAuthScheme.BASIC).
        post(json), Duration.Inf)

      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.OK
    }

    WsTestClient.withClient { implicit client =>
      val params = Map("protocol" -> "hdfs", "path" -> urlCSV, "format" -> "WRONG")
      val json = Json.toJson(params)

      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets").
        withAuth("david", "david", WSAuthScheme.BASIC).
        post(json), Duration.Inf)

      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.BAD_REQUEST
    }

    WsTestClient.withClient { implicit client =>

      val params = Map("protocol" -> "hdfs", "path" -> urlParquet)
      val json = Json.toJson(params)

      val response: WSResponse = Await.result[WSResponse](client.
        url(s"http://localhost:$port/storage-manager/v1/physical-datasets/schema").
        withAuth("david", "david", WSAuthScheme.BASIC).
        post(json), Duration.Inf)

      Logger.info(s"${response.status} ${response.body}")
      response.status shouldBe Status.OK

      response.body should not be "{}"

    }
  }

}

