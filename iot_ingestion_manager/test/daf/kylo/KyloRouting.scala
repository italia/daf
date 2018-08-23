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

package daf.kylo

import com.thinkbiganalytics.json.ObjectMapperSerializer
import daf.instances.Routing
import play.api.mvc.{ Action, AnyContent, Request, Result }

import scala.concurrent.Future
import scala.reflect.{ ClassTag, classTag }

trait KyloRouting extends Routing {

  private val feedManager     = MockFeedService.init()
  private val templateManager = MockTemplateService.init()

  private def json(request: Request[AnyContent]) = request.body.asJson match {
    case Some(json) => json
    case None       => throw new IllegalArgumentException(s"Invalid request body encountered while expecting JSON: [${request.body}]")
  }

  private def parsing[A: ClassTag](request: Request[AnyContent])(f: A => Future[Result]) = f {
    ObjectMapperSerializer.deserialize[A](
      json(request).toString,
      classTag[A].runtimeClass.asInstanceOf[Class[A]]
    )
  }

  private def parsingSync[A: ClassTag](request: Request[AnyContent])(f: A => Result) = parsing[A](request) { a => Future.successful(f(a)) }

  private def createFeedAction = Action.async {
    parsing(_) { feedManager.createFeed }
  }

  private def registeredTemplatesAction = Action { feedManager.registeredTemplates }

  private def allTemplatesAction = Action { templateManager.allTemplates }

  override protected def routes = {
    case ("POST", "/api/v1/feedmgr/feeds")                => createFeedAction
    case ("GET" , "/api/v1/feedmgr/templates/registered") => registeredTemplatesAction
    case ("GET" , "/nifi-api/flow/templates")             => allTemplatesAction
  }

}
