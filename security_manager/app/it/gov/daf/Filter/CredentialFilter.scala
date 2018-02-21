package it.gov.daf.Filter

import javax.inject.Inject

import akka.stream.Materializer
import it.gov.daf.common.sso.common.{CacheWrapper, CredentialManager}
import it.gov.daf.common.utils._
import org.slf4j.MDC
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class CredentialFilter@Inject() (implicit val mat: Materializer, ec: ExecutionContext, credentialManager:CredentialManager, cacheWrapper: CacheWrapper) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    if( requestHeader.headers.get("authorization").nonEmpty ) {

      val credentials = credentialManager.readCredentialFromRequest(requestHeader)
      MDC.put("user-id",credentials.username)
      credentials match {
        case Credentials(u, p, g) => cacheWrapper.deleteCredentials(u); cacheWrapper.putCredentials(u,p)
        case _ =>
      }

    }

    nextFilter(requestHeader).map{result => MDC.remove("user-id"); result }

  }



}