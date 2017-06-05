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

package modules

import io.swagger.config._
import io.swagger.models.Swagger
import io.swagger.models.auth.{BasicAuthDefinition, SecuritySchemeDefinition}
import play.api.Logger
import play.modules.swagger.PlayReader

import scala.collection.JavaConverters._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Var"
  )
)
object WithSecurityInfoApiListingCache {
  var cache: Option[Swagger] = None

  def listing(docRoot: String, host: String): Option[Swagger] = {
    Logger("swagger").debug("Loading API metadata")

    val scanner = ScannerFactory.getScanner
    val classes: java.util.Set[Class[_]] = scanner.classes()
    val reader = new PlayReader(null)
    var swagger = reader.read(classes)

    scanner match {
      case config: SwaggerConfig =>
        swagger = config.configure(swagger)
      case _ =>
      // no config, do nothing
    }
    val cache = Some(swagger)
    cache.fold(())(_.setHost(null))
    cache.fold(())(_.setSecurityDefinitions(Map[String, SecuritySchemeDefinition]("basicAuth" -> new BasicAuthDefinition()).asJava))
    cache
  }
}
