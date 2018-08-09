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

import io.swagger.config.FilterFactory
import io.swagger.core.filter.SpecFilter
import io.swagger.models.Swagger
import _root_.modules.WithSecurityInfoApiListingCache
import play.api.Logger
import play.api.mvc.RequestHeader

import scala.collection.JavaConverters._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.ImplicitParameter"
  )
)
class WithSecurityInfoApiHelpController extends ApiHelpController {
  override protected def getResourceListing(host: String)(implicit requestHeader: RequestHeader): Swagger = {
    Logger("swagger").debug("ApiHelpInventory.getRootResources")
    val docRoot = ""
    val queryParams = for ((key, value) <- requestHeader.queryString) yield {
      (key, value.toList.asJava)
    }
    val cookies = (for (cookie <- requestHeader.cookies) yield {
      (cookie.name, cookie.value)
    }).toMap
    val headers = for ((key, value) <- requestHeader.headers.toMap) yield {
      (key, value.toList.asJava)
    }

    val f = new SpecFilter
    val l: Option[Swagger] = WithSecurityInfoApiListingCache.listing(docRoot, host)

    val specs: Swagger = l match {
      case Some(m) => m
      case _ => new Swagger()
    }

    val hasFilter = Option(FilterFactory.getFilter)
    hasFilter match {
      case Some(_) => f.filter(specs, FilterFactory.getFilter, queryParams.asJava, cookies.asJava, headers.asJava)
      case None => specs
    }
  }

  override protected def getApiListing(resourceName: String, host: String)(implicit requestHeader: RequestHeader): Swagger = {
    Logger("swagger").debug("ApiHelpInventory.getResource(%s)".format(resourceName))
    val docRoot = ""
    val f = new SpecFilter
    val queryParams = requestHeader.queryString.map {case (key, value) => key -> value.toList.asJava}
    val cookies = requestHeader.cookies.map {cookie => cookie.name -> cookie.value}.toMap.asJava
    val headers = requestHeader.headers.toMap.map {case (key, value) => key -> value.toList.asJava}
    val pathPart = resourceName

    val l: Option[Swagger] = WithSecurityInfoApiListingCache.listing(docRoot, host)
    val specs: Swagger = l match {
      case Some(m) => m
      case _ => new Swagger()
    }
    val hasFilter = Option(FilterFactory.getFilter)

    val clone = hasFilter match {
      case Some(_) => f.filter(specs, FilterFactory.getFilter, queryParams.asJava, cookies, headers.asJava)
      case None => specs
    }
    clone.setPaths(clone.getPaths.asScala.filterKeys(_.startsWith(pathPart)).asJava)
    clone
  }
}
