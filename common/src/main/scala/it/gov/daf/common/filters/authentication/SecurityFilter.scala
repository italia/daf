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

package it.gov.daf.common.filters.authentication

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import org.pac4j.core.config.Config
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc._
import play.libs.concurrent.HttpExecutionContext

import scala.concurrent.Future

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Overloading"
  )
)
@Singleton
class SecurityFilter @Inject()(mat: Materializer, configuration: Configuration, playSessionStore: PlaySessionStore, config: Config, ec: HttpExecutionContext) extends org.pac4j.play.filters.SecurityFilter(mat, configuration, playSessionStore, config, ec) {

  override def apply(nextFilter: (RequestHeader) => Future[play.api.mvc.Result])
                    (request: RequestHeader): Future[play.api.mvc.Result] = {
    super.apply(nextFilter)(request)
  }
}
