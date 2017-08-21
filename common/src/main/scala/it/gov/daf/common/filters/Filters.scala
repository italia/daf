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

package it.gov.daf.common.filters

import javax.inject.Inject

import it.gov.daf.common.filters.authentication.SecurityFilter
import play.api.http.HttpFilters
import play.filters.cors.CORSFilter

class FiltersSecurity @Inject()(securityFilter: SecurityFilter) extends HttpFilters {

  def filters = Seq(securityFilter)

}

class FiltersSecurityCORS @Inject()(securityFilter: SecurityFilter, corsFilter: CORSFilter) extends HttpFilters {

  def filters = Seq(securityFilter, corsFilter)

}