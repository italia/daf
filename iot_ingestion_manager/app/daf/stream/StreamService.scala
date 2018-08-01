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

package daf.stream

import client.CatalogClient
import play.api.cache.CacheApi
import it.gov.daf.common.utils._
import representation.StreamData

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

class StreamService(cache: CacheApi, catalogClient: CatalogClient)(protected implicit val ec: ExecutionContext) {

  private def findCachedStreamData(id: String) = cache.get[StreamData] { s"stream:[$id]" }

  private def fetchStreamData(catalogId: String, auth: String) = for {
    catalog    <- catalogClient.getCatalog(catalogId, auth)
    streamData <- StreamData.fromCatalog(catalog).~>[Future]
  } yield streamData

  private def updateCaches(catalogId: String, streamData: StreamData) = cache.set(catalogId, streamData)

  private def updateStreamData(catalogId: String, auth: String) = fetchStreamData(catalogId, auth).andThen {
    case Success(streamData) => updateCaches(catalogId, streamData)
  }

  def findStreamData(id: String, auth: String) = findCachedStreamData(id).map { Future.successful } getOrElse updateStreamData(id, auth)

  def createStreamData(streamData: StreamData) = Future { updateCaches(streamData.id, streamData) }

}
