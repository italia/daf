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

package controllers

import akka.stream.ActorMaterializer
import controllers.modules.TestAbstractModule
import it.teamdigitale.instances.{ AkkaInstance, ConfigurationInstance }
import org.pac4j.core.profile.{ CommonProfile, ProfileManager }
import org.pac4j.play.PlayWebContext
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.Await
import scala.concurrent.duration._

class DatasetControllerSpec extends TestAbstractModule
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ConfigurationInstance
  with AkkaInstance {

  implicit lazy val executionContext = actorSystem.dispatchers.lookup("akka.actor.test-dispatcher")

  protected implicit lazy val materializer = ActorMaterializer.create { actorSystem }

  private def withController[U](f: DatasetController => U) = f { new DatasetController(configuration, sessionStore, ws, bodyParsers, actorSystem, executionContext) }

  private def request[A](method: String, uri: String, body: A, authorization: Option[String] = None, headers: Headers = Headers())(action: => Action[A]) = Await.result(
    action {
      FakeRequest(
        method  = method,
        uri     = uri,
        body    = body,
        headers = authorization.fold(headers) { auth => headers.add("Authorization" -> auth) }
      )
    },
    5.seconds
  )

  private val userProfile = {
    val profile = new CommonProfile
    profile.setId("test-user")
    profile.setRemembered(true)
    profile
  }

  private def createSession() = {
    val context = new PlayWebContext(
      FakeRequest(
        method  = "OPTIONS",
        uri     = "/",
        body    = AnyContentAsEmpty,
        headers = Headers()
      ),
      sessionStore
    )
    val profileManager = new ProfileManager[CommonProfile](context)
    profileManager.save(true, userProfile, false)
  }

  override def beforeAll() = {
    startAkka()
    createSession()
  }

  "A Dataset Controller" when {

    "calling bulk download" must {

      "return 401 when the auth header is missing" in withController { controller =>
        request[AnyContent]("GET", "/dataset-manager/v1/dataset/data/path", AnyContentAsEmpty) {
          controller.getDataset("data/path", "invalid")
        }.header.status should be { 401 }
      }

      "return 400 when format is invalid" in withController { controller =>
        request[AnyContent]("GET", "/dataset-manager/v1/dataset/data/path", AnyContentAsEmpty, Some("Basic:token")) {
          controller.getDataset("data/path", "invalid")
        }.header.status should be { 400 }
      }

      "return 404 when the data files do not exist" in withController { controller =>
        request[AnyContent]("GET", "/dataset-manager/v1/dataset/data/path", AnyContentAsEmpty, Some("Basic:token")) {
          controller.getDataset("data/path", "avro")
        }.header.status should be { 404 }
      }

    }

  }

}
