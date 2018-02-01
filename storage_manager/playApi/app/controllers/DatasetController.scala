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

package controllers

import com.google.inject.Inject
import io.swagger.annotations._
import daf.dataset.{DatasetService, Query}
import daf.dataset.json._
import org.apache.spark.streaming.Seconds
import org.pac4j.play.store.PlaySessionStore
import play.api.{Configuration, Logger}
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

@Api(value = "dataset-manager")
class DatasetController @Inject()(
                                   configuration: Configuration,
                                   playSessionStore: PlaySessionStore,
                                   ws: WSClient,
                                   implicit val ec: ExecutionContext
                                 ) extends AbstractController(configuration, playSessionStore) {

  private val datasetService = new DatasetService(configuration.underlying, ws)(ec)

  private val log = Logger(this.getClass)

  //  @ApiOperation(
  //    value = "Get a dataset based on the dataset id",
  //    produces = "application/json",
  //    httpMethod = "GET",
  //    authorizations = Array(new Authorization(value = "basicAuth")),
  //    protocols = "https, http"
  //  )
  //  def getSchema(
  //    @ApiParam(value = "the uri to access the dataset", required = true) uri: String
  //  ): Action[AnyContent] = Action.async { implicit request =>
  //    log.info(s"processing request=${request.method} with uri=$uri")
  //
  //    withAuthentication(request) { auth =>
  //      datasetService.schema(auth, uri)
  //        .map { st =>
  //          log.info(s"response request=${request.method} with value=$st")
  //          Ok(st.prettyJson)
  //        }
  //        .recover {
  //          case ex: Throwable =>
  //            log.error(s"processing request=${request.method} with uri=$uri error=${ex.getMessage}", ex)
  //            BadRequest(ex.getMessage).as(JSON)
  //        }
  //    }
  //  }

  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    httpMethod = "GET",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  def getSchema(
                 @ApiParam(value = "the uri to access the dataset", required = true) uri: String
               ): Action[AnyContent] =
    Action {
      CheckedAction(exceptionManager orElse hadoopExceptionManager) {
        HadoopDoAsAction {
          request =>
            log.info(s"processing request=${request.method} for schema")
            val auth = request.headers.get("Authorization").getOrElse("NO Authorization")
//            val res = datasetService.schema(auth, uri).map { st =>
//              log.info(s"response request=${request.method} with value=$st")
//              Ok(st.prettyJson)
//            }
//              .recover {
//                case ex: Throwable =>
//                  log.error(s"processing request=${request.method} with uri=$uri error=${ex.getMessage}", ex)
//                  BadRequest(ex.getMessage).as(JSON)
//              }
            val res = datasetService.schema(auth, uri) match {
              case Success(st) =>
                log.info(s"response request=${request.method} with value=$st")
                Ok(st.prettyJson)
              case Failure(ex) =>
                log.error(s"processing request=${request.method} with uri=$uri error=${ex.getMessage}", ex)
                BadRequest(ex.getMessage).as(JSON)
            }

            //Await.result(res, Duration.Inf);
            res
        }
      }
    }

  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    httpMethod = "GET",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  def getDataset(
                  @ApiParam(value = "the uri to access the dataset", required = true) uri: String
                ): Action[AnyContent] =
    Action {
      CheckedAction(exceptionManager orElse hadoopExceptionManager) {
        HadoopDoAsAction {
          request =>
            log.info(s"processing request=${request.method} with uri=$uri")
            val auth = request.headers.get("Authorization").getOrElse("NO Authorization")
            val res = datasetService.data(auth, uri) match {
              case Success(df) =>
                val records = s"[${df.toJSON.collect().mkString(",")}]"
                log.info(s"response request=${request.method} with records=$records")
                df.unpersist()
              Ok(records)
              case Failure(ex) =>
                  log.error(s"processing request=${request.method} with uri=$uri error=${ex.getMessage}", ex)
                  BadRequest(ex.getMessage).as(JSON)
              }
            res
        }
      }
    }

  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    consumes = "application/json",
    httpMethod = "POST",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      name = "query",
      value = "A valid query",
      required = true,
      dataType = "daf.dataset.Query",
      paramType = "body"
    )
  ))
  def queryDataset(
                    @ApiParam(value = "the uri to access the dataset", required = true) uri: String
                  ): Action[AnyContent] = Action {
    CheckedAction(exceptionManager orElse hadoopExceptionManager) {
      HadoopDoAsAction {
        request =>
          log.info(s"processing request=${request.method} with uri=$uri")
          val auth = request.headers.get("Authorization").getOrElse("NO Authorization");
          val query = request.body.asJson.map(_.as[Query])
          val res = query match {
            case Some(q) =>
              datasetService.query(auth, uri, q) match {
            case Success(df) =>
              val records = s"[${df.toJSON.collect().mkString(",")}]"
              log.info(s"response request=${request.method} with records=$records")
              df.unpersist()
              Ok(records)
            case Failure(ex) =>
              log.error(s"processing request=${request.method} with uri=$uri error=${ex.getMessage}", ex)
              BadRequest(ex.getMessage).as(JSON)
          }
            case None =>
              log.error(s"processing request=${request.method} with uri=$uri error=missing query body")
              BadRequest("Missing Query Body").as(JSON)
          }
          res;
      }
    }

  }




}
