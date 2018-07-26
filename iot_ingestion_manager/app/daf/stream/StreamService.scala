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
