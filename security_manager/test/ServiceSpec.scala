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
import java.util.Base64

import better.files._
import it.gov.daf.securitymanager.client.Security_managerClient
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.{HdfsConfiguration, MiniDFSCluster}
import org.apache.hadoop.test.PathUtils
import org.apache.spark.sql.SparkSession
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.{WSAuthScheme, WSResponse}
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

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

  def application: Application = GuiceApplicationBuilder().
    configure("hadoop_conf_dir" -> s"${ServiceSpec.confPath.pathAsString}").
    configure("pac4j.authenticator" -> "test").
    build()

  "The security_manager" should {
    "manage user tokens correctly" in new WithServer(app = application, port = getAvailablePort) {

      private val token = WsTestClient.withClient { implicit client =>
        val response: WSResponse = Await.result[WSResponse](client.
          url(s"http://localhost:$port/security-manager/v1/token").
          withAuth("david", "david", WSAuthScheme.BASIC).
          execute, Duration.Inf)
        response.body
      }

      val ws: AhcWSClient = AhcWSClient()

      val plainCreds = "david:david"
      val plainCredsBytes = plainCreds.getBytes
      val base64CredsBytes = Base64.getEncoder.encode(plainCredsBytes)
      val base64Creds = new String(base64CredsBytes)

      val client = new Security_managerClient(ws)(s"http://localhost:$port")

      Await.result(client.token(s"Basic $base64Creds").map(token => s""""$token""""), Duration.Inf) must be equalTo token

    }
  }

  override def beforeAll(): Unit = {
    val conf = new HdfsConfiguration()
    conf.setBoolean("dfs.permissions", true)
    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA)
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, testDataPath.pathAsString)
    conf.set(s"hadoop.proxyuser.${System.getProperties.get("user.name")}.groups", "*")
    conf.set(s"hadoop.proxyuser.${System.getProperties.get("user.name")}.hosts", "*")
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

  private val (testDataPath, confPath) = {
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