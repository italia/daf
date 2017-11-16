package it.gov.daf.play.security

import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Cookie

import scala.concurrent.{ExecutionContext, Future}
import protocol._

class ExternalServiceProxy(
  ws: WSClient,
  cache: CookieCache,
  ssoClient: SingleSignOnClient
)(implicit ec: ExecutionContext) {

  type HandledCall = Cookie => Future[WSResponse]

  def callService(credentials: ServiceCredentials)(serviceFun: HandledCall): Future[WSResponse] = {
    implicit val cred = credentials
   cache.retrieve(credentials)
      .flatMap(handleEmptyCookie)
      .flatMap(serviceFun)
      .flatMap(r => handleInvalidCookie(r, serviceFun))
  }

  /**
    *
    * @param optCookie
    * @param credentials
    * @return in case the cookie is empty perform a new cookie request
    */
  private def handleEmptyCookie(optCookie: Option[Cookie])(implicit credentials: ServiceCredentials): Future[Cookie] =
    optCookie match {
    case Some(cookie) =>
      Future.successful(cookie)
    case None =>
      ssoClient.createCookie(credentials)
          .flatMap(cookie => cache.insert(credentials, cookie))
  }

  /**
    *
    * @param response
    * @param credentials
    * @return
    */
  private def handleInvalidCookie(response: WSResponse, handledCall: HandledCall)(implicit credentials: ServiceCredentials) = {
    response.status match {
      case 401 =>
        //Unauthorized
        cache.delete(credentials)
            .flatMap(_ => ssoClient.createCookie(credentials))
            .flatMap(cache.insert(credentials, _))
              .flatMap(handledCall)

      case 200 =>
        Future.successful(response)

      case other =>
        Future.failed(new Throwable(s"got response with status ${response.status}"))

    }
  }
}
