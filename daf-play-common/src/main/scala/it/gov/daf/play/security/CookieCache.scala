package it.gov.daf.play.security

import com.github.benmanes.caffeine.cache.Caffeine
import play.api.mvc.Cookie

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.modes.scalaFuture._
import protocol._

object CookieCache {
  def apply(cache: Cache[Cookie], jwtTimeToLive: Duration)(implicit ec: ExecutionContext): CookieCache =
    new CookieCache(cache, jwtTimeToLive)(ec)

  def apply(jwtTimeToLive: Duration)(implicit ec: ExecutionContext) = {
    val underlying = Caffeine.newBuilder().maximumSize(10000L).build[String, Entry[Cookie]]
    new CookieCache(CaffeineCache(underlying), jwtTimeToLive)(ec)
  }
}

class CookieCache(cache: Cache[Cookie], jwtTimeToLive: Duration)
                 (implicit ec: ExecutionContext) {

  private implicit val c = cache

  private def key(credentials: ServiceCredentials) =
    s"${credentials.appName}-${credentials.username}"

  def insert(credentials: ServiceCredentials, cookie: Cookie): Future[Cookie] = {
    put(key(credentials))(cookie, ttl = Some(jwtTimeToLive))
      .map(_ => cookie)
  }

  def retrieve(credentials: ServiceCredentials): Future[Option[Cookie]] =
    get(key(credentials))

  def delete(credentials: ServiceCredentials) = remove(key(credentials))
}
