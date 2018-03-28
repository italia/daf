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

import it.gov.daf.securitymanager.client.Security_managerClient
import org.specs2.mutable.Specification
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.{WSAuthScheme, WSResponse}
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration.Duration

//@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Throw"))
class ServiceSpec extends Specification {

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

      val token2 = Await.result(client.token(s"Basic $base64Creds"), Duration.Inf)

      s""""$token2"""" must be equalTo token

      Await.result(client.token(s"Bearer $token2").map(token => s""""$token""""), Duration.Inf) must be equalTo token
    }
  }

}