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

import javax.inject._

import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router

import scala.concurrent.Future

import de.zalando.play.controllers.PlayBodyParsing


/**
  * The purpose of this ErrorHandler is to override default play's error reporting with application/json content type.
  */
//@SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.ExplicitImplicitTypes"))
class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                             ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  private def contentType(request: RequestHeader): String =
    request.acceptedTypes.map(_.toString).filterNot(_ == "text/html").headOption.getOrElse("application/json")

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    implicit val writer = PlayBodyParsing.anyToWritable[Throwable](contentType(request))
    Future.successful(InternalServerError(exception))
  }


  override def onDevServerError(request: RequestHeader, exception: UsefulException) = {
    implicit val writer = PlayBodyParsing.anyToWritable[Throwable](contentType(request))
    Future.successful(InternalServerError(exception))
  }


  // called when a route is found, but it was not possible to bind the request parameters
  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    implicit val writer = PlayBodyParsing.anyToWritable[String](contentType(request))
    Future.successful(BadRequest("Bad Request: " + error))
  }

  // 404 - page not found error
  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    implicit val writer = PlayBodyParsing.anyToWritable[String](contentType(request))
    Future.successful(NotFound(request.path))
  }
}