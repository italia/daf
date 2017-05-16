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

package it.gov.daf.common.authentication

import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.core.util.CommonHelper
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.profile.JwtGenerator
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result, Results}

import scala.collection.JavaConversions.{asScalaBuffer, _}

object Authentication extends Results {

  def getProfiles(request: RequestHeader, playSessionStore: PlaySessionStore): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  def getToken: (Configuration) => (PlaySessionStore) => (RequestHeader) => Result = (configuration: Configuration) => (playSessionStore: PlaySessionStore) => (request: RequestHeader) => {
    val secret = configuration.getString("pac4j.jwt_secret").fold[String](throw new Exception("missing secret"))(identity)
    val generator = new JwtGenerator[CommonProfile](new SecretSignatureConfiguration(secret))
    var token: String = ""
    val profiles = getProfiles(request, playSessionStore)
    if (CommonHelper.isNotEmpty(profiles)) {
      token = generator.generate(profiles.get(0))
    }
    Ok(Json.toJson(token))
  }

}
