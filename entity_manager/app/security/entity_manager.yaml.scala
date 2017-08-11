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

package entity_manager.yaml

import de.zalando.play.controllers.ResponseWriters
import de.zalando.play.controllers.SwaggerSecurityExtractors._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

object SecurityExtractorsExecutionContext {
  // this ExecutionContext might be overridden if default configuration is not suitable for some reason
  implicit val ec: ExecutionContext = de.zalando.play.controllers.Contexts.tokenChecking
}

trait SecurityExtractors {
  def basicAuth_Extractor[User >: Any](): RequestHeader => Future[Option[User]] =
    header => basicAuth(header) {
      (username: String, password: String) => {
      }
    }

  implicit val unauthorizedContentWriter: ResponseWriters.choose[String] = ResponseWriters.choose[String]("application/json")

  def unauthorizedContent(req: RequestHeader) = Results.Unauthorized("Unauthorized")
}
