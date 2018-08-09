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

package controllers.modules

import akka.stream.Materializer
import daf.util.TestCache
import org.pac4j.play.store.{ PlayCacheSessionStore, PlaySessionStore }
import play.api.{ Configuration, Environment }
import play.api.inject.Module
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.BodyParsers
import play.cache.{ CacheApi, DefaultCacheApi }
import play.api.cache.{ CacheApi => ApiCacheApi }
import play.api.inject.guice.GuiceInjectorBuilder

abstract class TestAbstractModule extends Module {

  private lazy val injector = new GuiceInjectorBuilder().bindings(this).injector()

  private lazy val sessionStoreInstance: PlaySessionStore = injector.instanceOf { classOf[PlaySessionStore] }
  private lazy val wsClientInstance: WSClient = injector.instanceOf { classOf[WSClient] }
  private lazy val bodyParsersInstance: BodyParsers = BodyParsers

  final def sessionStore: PlaySessionStore = sessionStoreInstance
  final def ws: WSClient = wsClientInstance
  final def bodyParsers: BodyParsers = bodyParsersInstance

  protected implicit def materializer: Materializer

  override def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind(classOf[ApiCacheApi]).to(classOf[TestCache]),
    bind(classOf[CacheApi]).to(classOf[DefaultCacheApi]),
    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore]),
    bind(classOf[BodyParsers]).to(BodyParsers),
    bind(classOf[WSClient]).toInstance(AhcWSClient())
  )
}
