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
import org.apache.avro.SchemaBuilder
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.{HdfsConfiguration, MiniDFSCluster}
import org.apache.hadoop.test.PathUtils
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Try}

final case class Address(stret: String)

final case class Person(name: String, age: Int, address: Address)

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Throw"))
class ServiceSpec extends Specification with BeforeAfterAll {

  import ServiceSpec._

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

  def application: Application = GuiceApplicationBuilder().configure("hadoop_conf_dir" -> s"${ServiceSpec.confPath.pathAsString}").build()

  val limit = 1000

  "The storage_manager" should {
    "manage the physical datasets correctly" in new WithServer(app = application, port = getAvailablePort) {
      val (doc, txtDoc, schema) = {
        val sparkSession = ServiceSpec.sparkSession

        import sparkSession.implicits._

        val persons = (1 to limit).map(i => Person(s"Andy$i", Random.nextInt(85), Address("Via Delle Benedettine 47")))
        val caseClassDS = persons.toDS()
        caseClassDS.write.format("parquet").mode(SaveMode.Overwrite).save("/opendata/test.parquet")
        caseClassDS.write.format("com.databricks.spark.avro").mode(SaveMode.Overwrite).save("/opendata/test.avro")
        caseClassDS.toDF.select("name", "age").write.format("csv").mode(SaveMode.Overwrite).save("/opendata/test.csv")
        val txtDoc = caseClassDS.take(limit).map(p => s"${p.name},${p.age}").mkString("\n")
        val doc = s"[${
          caseClassDS.toDF().take(limit).map(row => {
            Utility.rowToJson(caseClassDS.schema)(row)
          }).mkString(",")
        }]"
        val df = sparkSession.read.format("parquet").load("/opendata/test.parquet")
        val schema = SchemaConverters.convertStructToAvro(df.schema, SchemaBuilder.record("topLevelRecord"), "").toString(true)
        (doc, txtDoc, schema)
      }

      WsTestClient.withClient { implicit client =>
        val uri = "dataset:hdfs:/opendata/test.parquet"
        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=parquet&limit=$limit").execute, Duration.Inf)
        response.body must be equalTo doc
      }

      WsTestClient.withClient { implicit client =>
        val uri = "dataset:hdfs:/opendata/test.avro"
        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=avro&limit=$limit").execute, Duration.Inf)
        response.body must be equalTo doc
      }

      WsTestClient.withClient { implicit client =>
        val uri = "dataset:hdfs:/opendata/test.csv"
        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=text&limit=$limit").execute, Duration.Inf)
        response.body must be equalTo txtDoc
      }

      WsTestClient.withClient { implicit client =>
        val uri = "dataset:hdfs:/opendata/test.avro"
        val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage-manager/v1/physical-datasets/schema?uri=$uri&format=avro").execute, Duration.Inf)
        response.body must be equalTo schema
      }
    }
  }

  override def beforeAll(): Unit = {
    val conf = new HdfsConfiguration()
    conf.setBoolean("dfs.permissions", true)
    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA)
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, testDataPath.pathAsString)
    val builder = new MiniDFSCluster.Builder(conf)
    miniCluster = Try(builder.build())
    fileSystem = miniCluster.map(_.getFileSystem)
    fileSystem.foreach(fs => {
      val confFile: File = confPath / "hdfs-site.xml"
      for {os <- confFile.newOutputStream.autoClosed} fs.getConf.writeXml(os)
    })
  }

  override def afterAll(): Unit = {
    miniCluster.foreach(_.shutdown(true))
    val _ = testDataPath.parent.parent.delete(true)
    sparkSession.stop()
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
object ServiceSpec {

  val (testDataPath, confPath): (File, File) = {
    val testDataPath = s"${PathUtils.getTestDir(classOf[ServiceSpec]).getCanonicalPath}/MiniCluster"
    val confPath = s"$testDataPath/conf"
    (
      testDataPath.toFile.createIfNotExists(asDirectory = true, createParents = false),
      confPath.toFile.createIfNotExists(asDirectory = true, createParents = false)
    )
  }

  var miniCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)

  var fileSystem: Try[FileSystem] = Failure[FileSystem](new Exception)

  lazy val sparkSession: SparkSession = SparkSession.builder().master("local").getOrCreate()

}