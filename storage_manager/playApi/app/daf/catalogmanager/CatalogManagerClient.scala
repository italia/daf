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

import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http.Http

class CatalogManagerClient(serviceUrl: String)(implicit val ec: ExecutionContext) {
  import json._

  def datasetCatalogByUid(authorization: String, catalogId: String): Future[MetaCatalog] = Future {
    val resp = Http(s"$serviceUrl/catalog-manager/v1/catalog-ds/get/${URLEncoder.encode(catalogId,"UTF-8")}")
      .header("Authorization", authorization)
      .asString

    if (resp.is2xx) Json.parse(resp.body).as[MetaCatalog]
    else throw new java.lang.RuntimeException("unexpected response status: " + resp.statusLine + " " + resp.body)
  }

}
