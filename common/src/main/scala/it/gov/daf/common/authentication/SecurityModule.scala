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

import com.google.inject.{AbstractModule, Singleton}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.{DirectBasicAuthClient, HeaderClient}
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import play.api.{Configuration, Environment}

@Singleton
class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])

    val directBasicAuthClient = new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator)

    val secret = configuration.getString("pac4j.jwt_secret").fold[String](throw new Exception("missing secret"))(identity)

    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(secret))

    val parameterClient = new HeaderClient("Authorization", "Bearer ", jwtAuthenticator)

    val config = new Config(new Clients(directBasicAuthClient, parameterClient))

    config.setHttpActionAdapter(new DefaultHttpActionAdapter())

    bind(classOf[Config]).toInstance(config)
  }
}