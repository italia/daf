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

package daf.instances

import cats.free.Free
import cats.Id
import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import play.api.mvc.Handler
import play.api.test.{ FakeApplication, TestServer }
import java.util.{ List => JavaList }

import akka.stream.Materializer
import daf.util.TestApplicationInstances
import org.pac4j.play.store.PlaySessionStore
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient

import scala.collection.convert.decorateAsScala._
import scala.concurrent.ExecutionContext

trait Routing {

  protected def routes: PartialFunction[(String, String), Handler]

}

trait NoRouting extends Routing {

  final protected def routes = PartialFunction.empty[(String, String), Handler]

}

trait PlayInstance { this: Routing with ConfigurationInstance =>

  protected def dispatcherName: String = "test-dispatcher"

  private type Trampoline[A] = Free[Id, A]

  protected def serverPort = 19001

  private def readConfigValue(any: Any): Trampoline[AnyRef] = any match {
    case list: JavaList[_] => list.asScala.toList.traverse[Trampoline, AnyRef] { a => Free.defer(readConfigValue(a)) }.map { identity[AnyRef] }
    case anyRef: AnyRef    => Free.pure { anyRef }
    case unknown           => throw new IllegalArgumentException(s"Unhandled configuration type [${unknown.getClass.getName}] for value [$unknown]")
  }

  private val configurationMap = configuration.entrySet.toList.map { case (k, v) => k -> v.unwrapped }.traverse[Trampoline, (String, AnyRef)] {
    case (key, value) => readConfigValue(value).map { key -> _ }
  }.run.toMap[String, AnyRef]

  protected lazy val application = FakeApplication(
    withRoutes = routes,
    additionalConfiguration = configurationMap
  )

  protected lazy val server = TestServer(
    port        = serverPort,
    application = application
  )

  private lazy val instances = new TestApplicationInstances(application, dispatcherName)

  implicit def executionContext: ExecutionContext = instances.executionContext
  implicit def materializer: Materializer         = instances.materializer

  def ws: WSClient                   = instances.ws
  def cache: CacheApi                = instances.cache
  def sessionStore: PlaySessionStore = instances.sessionStore

  protected def startPlay() = {
    server.start()
  }

  protected def stopPlay() = {
    server.stop()
  }

}
