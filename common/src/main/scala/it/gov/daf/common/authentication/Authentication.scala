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
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.jwt.profile.JwtGenerator
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc.{RequestHeader, Result, Results}

import scala.collection.convert.decorateAsScala._
import scala.collection.mutable

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Var"
  )
)
object Authentication extends Results {

  var configuration: Option[Configuration] = None
  var playSessionStore: Option[PlaySessionStore] = None

  def apply(configuration: Configuration, playSessionStore: PlaySessionStore): Unit = {
    this.configuration = Some(configuration)
    this.playSessionStore = Some(playSessionStore)
  }

  def getClaims(requestHeader: RequestHeader): Option[mutable.Map[String, AnyRef]] = {
    val header: Option[String] = requestHeader.headers.get("Authorization")
    val token: Option[String] = for {
      h <- header
      t <- h.split("Bearer").lastOption
    } yield t.trim
    val secret: Option[String] = configuration.flatMap(_.getString("pac4j.jwt_secret"))
    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(secret.getOrElse(throw new Exception("missing secret"))))
    token.map(jwtAuthenticator.validateTokenAndGetClaims(_).asScala)
  }

  def getProfiles(request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore.getOrElse(throw new Exception("missing playSessionStore")))
    val profileManager = new ProfileManager[CommonProfile](webContext)
    profileManager.getAll(true).asScala.toList
  }

  def getToken: (RequestHeader) => Result = (request: RequestHeader) => {
    val secret = configuration.flatMap(_.getString("pac4j.jwt_secret"))
    val generator = new JwtGenerator[CommonProfile](new SecretSignatureConfiguration(secret.getOrElse(throw new Exception("missing secret"))))
    val profiles = getProfiles(request)
    val token: Option[String] = profiles.headOption.map(profile => {
      //val claims = new JWTClaimsSet.Builder().expirationTime(new Date).build()
      //profile.addAttributes(claims.getClaims) //TODO It's possible here to add expiration and other token related info
      generator.generate(profile)
    })
    Ok(token.getOrElse(""))
  }

}
