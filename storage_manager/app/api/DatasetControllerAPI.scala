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

import daf.dataset.query.Query
import io.swagger.annotations._
import javax.ws.rs.QueryParam
import play.api.mvc.{ Action, AnyContent }

@Api(value = "dataset-manager")
trait DatasetControllerAPI {

  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    httpMethod = "GET",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  def getSchema(@ApiParam(value = "the uri to access the dataset", required = true) uri: String): Action[AnyContent]


  @ApiOperation(
    value = "Get a dataset based on the dataset id",
    produces = "application/json",
    httpMethod = "GET",
    authorizations = Array(new Authorization(value = "basicAuth")),
    protocols = "https, http"
  )
  def getDataset(@ApiParam(value = "the uri to access the dataset", required = true)
                 uri: String,
                 @ApiParam(value = "the format the downloaded data should be converted", required = false, defaultValue = "json")
                 @QueryParam("format")
                 format: String,
                 @ApiParam(value = "the method used to perform the data conversions", required = true)
                 @QueryParam("method")
                 method: String): Action[AnyContent]

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
  def queryDataset(@ApiParam(value = "the uri to access the dataset", required = true)
                   uri: String,
                   @ApiParam(value = "the format the downloaded data should be converted", required = false, defaultValue = "json")
                   @QueryParam("format")
                   format: String,
                   @ApiParam(value = "the method used to perform the data conversions", required = true)
                   @QueryParam("method")
                   method: String): Action[Query]

}
