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

package daf.catalogmanager

import java.net.URLEncoder
import java.security.AccessControlException

import it.gov.daf.common.config.Read
import json._
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.Json
import scalaj.http.{ Http, HttpResponse }

import scala.util.{ Failure, Try, Success => TrySuccess }

class CatalogManagerClient(serviceUrl: String) {

  val logger = LoggerFactory.getLogger("it.gov.daf.CatalogManager")

  private def callService(authorization: String, catalogId: String) = Try {
    Http(s"$serviceUrl/catalog-manager/v1/catalog-ds/get/${URLEncoder.encode(catalogId,"UTF-8")}")
      .header("Authorization", authorization)
      .asString
  }

  private def parseCatalog(response: HttpResponse[String]) =
    if (response.code == 401)  Failure { new AccessControlException("Unauthorized") }
    else if (response.isError) Failure { new RuntimeException(s"Error retrieving catalog data: [${response.code}] with body [${response.body}]") }
    else Try { Json.parse(response.body).as[MetaCatalog] }

  def getById(authorization: String, catalogId: String): Try[MetaCatalog] = for {
    response <- callService(authorization, catalogId)
    catalog  <- parseCatalog(response)
  } yield catalog

}

object CatalogManagerClient {

  def fromConfig(config: Configuration) = Read.string { "daf.catalog-url" }.!.read(config) match {
    case TrySuccess(baseUrl) => new CatalogManagerClient(baseUrl)
    case Failure(error)      => throw new RuntimeException("Unable to create catalog-manager client", error)
  }

}