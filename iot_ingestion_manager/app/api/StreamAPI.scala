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

package api

import io.swagger.annotations._
import it.gov.daf.common.web.SecureController
import javax.ws.rs.PathParam
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc.{ Action, AnyContent }
import representation.{ Event, StreamData }

@Api(value = "iot-manager")
abstract class StreamAPI(override protected val configuration: Configuration,
                         override val playSessionStore: PlaySessionStore) extends SecureController(configuration, playSessionStore) {

  @ApiOperation(
    value      = "Register a stream using the information POSTed in the request body",
    consumes   = "application/json",
    produces   = "application/json",
    httpMethod = "POST",
    protocols  = "https, http",
    code       = 201
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "stream-data",
        dataType  = "representation.StreamData",
        paramType = "body",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, message = "The stream has been created successfully", response = classOf[Void])
    )
  )
  def register: Action[StreamData]

  @ApiOperation(
    value      = "Register a stream by deriving information from the catalog",
    produces   = "application/json",
    httpMethod = "POST",
    protocols  = "https, http",
    code       = 201
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 201, message = "The stream has been created successfully", response = classOf[Void]),
      new ApiResponse(code = 400, message = "A catalog with this id does exist, but is not eligible for streaming"),
      new ApiResponse(code = 404, message = "No catalog exists with the supplied id")
    )
  )
  def registerCatalog(@ApiParam(value = "id of the catalog entry to register", required = true)
                      @PathParam("catalogId")
                      catalogId: String): Action[AnyContent]

  @ApiOperation(
    value      = "Push an event onto the queue for the stream corresponding to the catalog entry identified by the provided id",
    consumes   = "application/json",
    produces   = "application/json",
    httpMethod = "PUT",
    protocols  = "https, http"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name      = "event",
        dataType  = "representation.Event",
        paramType = "body",
        required  = true
      )
    )
  )
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "The message has been pushed successfully", response = classOf[Void]),
      new ApiResponse(code = 400, message = "A catalog with this id does exist, but is not eligible for streaming"),
      new ApiResponse(code = 404, message = "No catalog exists with the supplied id")
    )
  )
  def update(@ApiParam(value = "id of the catalog to whose queue the event should be pushed", required = true)
             @PathParam("catalogId")
             catalogId: String): Action[Event]

}
