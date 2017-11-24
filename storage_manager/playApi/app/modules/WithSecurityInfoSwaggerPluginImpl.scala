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

package modules

import javax.inject.Inject

import play.api.Application
import play.api.inject.ApplicationLifecycle
import play.api.routing.Router

class WithSecurityInfoSwaggerPluginImpl @Inject()(lifecycle: ApplicationLifecycle, router: Router, app: Application) extends play.modules.swagger.SwaggerPluginImpl(lifecycle, router, app) {
  override val docRoot: String = ""
  private val _ = WithSecurityInfoApiListingCache.listing(docRoot, "127.0.0.1")
}