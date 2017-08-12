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

import java.io.{File, FileNotFoundException, IOException}
import java.net.ServerSocket
import java.util.Base64

import it.gov.daf.entitymanager.Entity
import it.gov.daf.entitymanager.client.Entity_managerClient
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test.WithServer

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Var"
  )
)
class ServiceSpec extends Specification with BeforeAfterAll {

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

  private def constructTempDir(dirPrefix: String): Try[File] = Try {
    val rndrange = 10000000
    val file = new File(System.getProperty("java.io.tmpdir"), s"$dirPrefix${Random.nextInt(rndrange)}")
    if (!file.mkdirs())
      throw new RuntimeException("could not create temp directory: " + file.getAbsolutePath)
    file.deleteOnExit()
    file
  }

  private def deleteDirectory(path: File): Boolean = {
    if (!path.exists()) {
      throw new FileNotFoundException(path.getAbsolutePath)
    }
    var ret = true
    if (path.isDirectory)
      path.listFiles().foreach(f => ret = ret && deleteDirectory(f))
    ret && path.delete()
  }

  var tmpDir: Try[File] = Failure[File](new Exception(""))
  
  def application: Application = GuiceApplicationBuilder().
    configure("pac4j.authenticator" -> "test").
    configure("janusgraph.storage.directory" -> s"${tmpDir.map(_.getCanonicalPath).getOrElse("db")}/berkeleyje").
    configure("janusgraph.index.search.directory" -> s"${tmpDir.map(_.getCanonicalPath).getOrElse("db")}/lucene").
    build()

  "The entity_manager" should {
    "create an entity and retrieve it correctly" in new WithServer(app = application, port = getAvailablePort) {

      val ws: AhcWSClient = AhcWSClient()

      val plainCreds = "david:david"
      val plainCredsBytes = plainCreds.getBytes
      val base64CredsBytes = Base64.getEncoder.encode(plainCredsBytes)
      val base64Creds = new String(base64CredsBytes)

      val client = new Entity_managerClient(ws)(s"http://localhost:$port")

      val result = Await.result(client.createEntity(s"Basic $base64Creds", Entity("DAVID")), Duration.Inf)
      val entity = Await.result(client.getEntity(s"Basic $base64Creds", "DAVID"), Duration.Inf)

      entity must beEqualTo(Entity("DAVID"))
    }
  }

  override def beforeAll(): Unit = tmpDir = constructTempDir("test")

  override def afterAll(): Unit = tmpDir.foreach(deleteDirectory(_))
}