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
import it.gov.daf.iotmanager.client.Iot_managerClient
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

import org.apache.solr.client.solrj.embedded.JettyConfig
import org.apache.solr.client.solrj.embedded.JettySolrRunner
import org.eclipse.jetty.servlet.ServletHolder


@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Throw"))
class Iot_managerSpec extends Specification with BeforeAfterAll {

  import Iot_managerSpec._

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
      print("ciao ciao")


    }
  }

  override def beforeAll(): Unit = {
    val solrXml = new Nothing("/solr/home/solr.xml")
    val solrHomeDir = solrXml.getParentFile

    val port = 8080
    val context = "/solr"
    // use org.apache.solr.client.solrj.embedded.JettySolrRunner
    val jettySolr = new Nothing(solrHomeDir.getAbsolutePath, context, port)

    val waitUntilTheSolrWebAppHasStarted = true
    jettySolr.start(waitUntilTheSolrWebAppHasStarted)
  }

  override def afterAll(): Unit = {
    jettySolr.stop()
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
object Iot_managerSpec {



}