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

import better.files._
import com.databricks.spark.avro.SchemaConverters
import controllers.Utility
import it.teamdigitale.baseSpec.HDFSbase
import org.apache.avro.SchemaBuilder
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSAuthScheme, WSResponse}
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Try}

final case class Address(stret: String)

final case class Person(name: String, age: Int, address: Address)

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Throw"))
class ServiceSpec extends HDFSbase {

  val url = super.pathParquet


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




  "The storage_manager" should "manage the physical datasets correctly" in new WithServer( port = getAvailablePort) {
      private val (doc, txtDoc, schema) = {

      WsTestClient.withClient { implicit client =>
        val uri = "dataset:hdfs:/opendata/test.parquet"
        val params = Map("protocol" -> "hdfs", "path" -> url)
        val response: WSResponse = Await.result[WSResponse](client.
          url(s"http://localhost:$port/storage-manager/v1/physical-datasets")
          .post(params), Duration.Inf)
        response.status === Status.UNAUTHORIZED
      }

//      //Test with a wrong dataset URI
//      WsTestClient.withClient { implicit client =>
//        val uri = "dataset:hdfs:/opendata/WRONG.parquet"
//        val response: WSResponse = Await.result[WSResponse](client.
//          url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=parquet&limit=$limit").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.status must be equalTo Status.NOT_FOUND
//      }
//
//      //Test with a wrong dataset scheme
//      WsTestClient.withClient { implicit client =>
//        val uri = "dataset:WRONG:/opendata/WRONG.parquet"
//        val response: WSResponse = Await.result[WSResponse](client.
//          url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=parquet&limit=$limit").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.status must be equalTo Status.NOT_IMPLEMENTED
//      }
//
//      WsTestClient.withClient { implicit client =>
//        val uri = "dataset:hdfs:/opendata/test.parquet"
//        val response: WSResponse = Await.result[WSResponse](client.
//          url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=parquet&limit=$limit").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.body must be equalTo doc
//      }
//
//      WsTestClient.withClient { implicit client =>
//        val uri = "dataset:hdfs:/opendata/test.parquet"
//        val response: WSResponse = Await.result[WSResponse](client.
//          url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=parquet&limit=$limit").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.body must be equalTo doc
//      }
//
//      WsTestClient.withClient { implicit client =>
//        val uri = "dataset:hdfs:/opendata/test.avro"
//        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=avro&limit=$limit").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.body must be equalTo doc
//      }
//
//      WsTestClient.withClient { implicit client =>
//        val uri = "dataset:hdfs:/opendata/test.csv"
//        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=text&limit=$limit").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.body must be equalTo txtDoc
//      }
//
//      WsTestClient.withClient { implicit client =>
//        val uri = "dataset:hdfs:/opendata/test.avro"
//        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets/schema?uri=$uri&format=avro").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.body must be equalTo schema
//      }
//
//      WsTestClient.withClient { implicit client =>
//        val files = fileSystem.map(fs => fs.listFiles(new Path("/opendata/test.csv"), false))
//        val csvFileName = files.map(files => {
//          files.next()
//          files.next().getPath.getName
//        }).getOrElse("")
//        val uri = s"dataset:hdfs:/opendata/test.csv/$csvFileName"
//        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=raw&limit=$limit").
//          withAuth("david", "david", WSAuthScheme.BASIC).
//          execute, Duration.Inf)
//        response.body.stripLineEnd must be equalTo txtDoc
//      }

    }

}

