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

package client

import java.net.URLEncoder

import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.catalogmanager.client.{ Catalog_managerClient => CatalogManager }
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.Success

class CatalogClient(ws: WSClient, cache: CacheApi, catalogUrl: String)(protected implicit val ec: ExecutionContext) {

  val client = new CatalogManager(ws)(catalogUrl)

  private def findAuthData(catalogId: String, auth: String) = cache.get[String] { s"catalog:[$catalogId]:auth:[$auth]" }

  private def findCatalogData(catalogId: String) = cache.get[MetaCatalog] { s"catalog:[$catalogId]" }

  private def findCatalog(catalogId: String, auth: String) = for {
    _           <- findAuthData(catalogId, auth)
    catalogData <- findCatalogData(catalogId)
  } yield catalogData

  private def updateCaches(catalogId: String, catalogData: MetaCatalog, auth: String) = {
    cache.set(s"catalog:[$catalogId]", catalogData, 1.hour)
    cache.set(s"catalog:[$catalogId]:auth:[$auth]", 1.hour)
  }

  private def updateCatalogData(catalogId: String, auth: String) = client.datasetcatalogbyid(
    auth,
    URLEncoder.encode(catalogId, "UTF-8")
  ).andThen {
    case Success(catalogData) => updateCaches(catalogId, catalogData, auth)
  }

  def getCatalog(catalogId: String, auth: String): Future[MetaCatalog] = findCatalog(catalogId, auth).map { Future.successful }.getOrElse {
    updateCatalogData(catalogId, auth)
  }

}
