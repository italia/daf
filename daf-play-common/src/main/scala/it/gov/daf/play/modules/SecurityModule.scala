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

package it.gov.daf.play.modules

import java.security.InvalidParameterException
import java.time.Duration
import javax.inject.Inject

import com.google.inject.{AbstractModule, Singleton}
import org.ldaptive.auth.{Authenticator, BindAuthenticationHandler, SearchDnResolver}
import org.ldaptive.pool._
import org.ldaptive.ssl.SslConfig
import org.ldaptive.{BindConnectionInitializer, ConnectionConfig, Credential, DefaultConnectionFactory}
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.{DirectBasicAuthClient, HeaderClient}
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.ldap.profile.service.LdapProfileService
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import play.api.{Configuration, Environment}


@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Overloading",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Null"
  )
)
@Singleton
class SecurityModule @Inject()(environment: Environment, configuration: Configuration) extends AbstractModule {

  private def getLdapAuthenticator = {
    /*
    val dnResolver = new FormatDnResolver
    //dnResolver.setFormat(configuration.getString("pac4j.ldap.user_dn_pattern").getOrElse(""))
    dnResolver.setFormat("uid=%s,cn=users,cn=accounts,dc=example,dc=test")
    */

    println("--> v.1.1 snapshot")
    val connectionConfig = new ConnectionConfig
    connectionConfig.setConnectTimeout(Duration.ofMillis(configuration.getLong("pac4j.ldap.connect_timeout").getOrElse(500)))
    connectionConfig.setResponseTimeout(Duration.ofMillis(configuration.getLong("pac4j.ldap.response_timeout").getOrElse(1000)))
    connectionConfig.setLdapUrl(
      configuration.getString("pac4j.ldap.url").getOrElse(throw new InvalidParameterException(s"Missing mandatory parameter pac4j.ldap.url"))
    )
    connectionConfig.setConnectionInitializer(
      new BindConnectionInitializer(configuration.getString("pac4j.ldap.bind_dn").
        getOrElse(throw new InvalidParameterException(s"Missing mandatory pac4j.ldap.bind_dn")),
        new Credential(configuration.getString("pac4j.ldap.bind_pwd").
          getOrElse(throw new InvalidParameterException(s"Missing mandatory pac4j.ldap.bind_pwd")))))
    connectionConfig.setUseSSL(true) //TODO Shall we keep SSL mandatory
    val sslConfig = new SslConfig()
    sslConfig.setTrustManagers() //TODO no more certificate validation, shall we keep it in this way?
    connectionConfig.setSslConfig(sslConfig)


    val connectionFactory = new DefaultConnectionFactory
    connectionFactory.setConnectionConfig(connectionConfig)
    val poolConfig = new PoolConfig
    poolConfig.setMinPoolSize(1)
    poolConfig.setMaxPoolSize(2)
    poolConfig.setValidateOnCheckOut(true)
    poolConfig.setValidateOnCheckIn(true)
    poolConfig.setValidatePeriodically(false)
    val searchValidator = new SearchValidator
    val pruneStrategy = new IdlePruneStrategy
    val connectionPool = new BlockingConnectionPool
    connectionPool.setPoolConfig(poolConfig)
    connectionPool.setBlockWaitTime(Duration.ofMillis(1000))
    connectionPool.setValidator(searchValidator)
    connectionPool.setPruneStrategy(pruneStrategy)
    connectionPool.setConnectionFactory(connectionFactory)
    connectionPool.initialize()
    val pooledConnectionFactory = new PooledConnectionFactory
    pooledConnectionFactory.setConnectionPool(connectionPool)

    val handler = new BindAuthenticationHandler( new DefaultConnectionFactory(connectionConfig) )

    val dnResolver = new SearchDnResolver(pooledConnectionFactory);
    dnResolver.setBaseDn( configuration.getString("pac4j.ldap.base_user_dn").getOrElse("xxxx") )
    val usernameAttribute = configuration.getString("pac4j.ldap.login_attribute").getOrElse("xxxx")
    dnResolver.setUserFilter(s"($usernameAttribute={user})");

    val ldaptiveAuthenticator = new Authenticator
    ldaptiveAuthenticator.setDnResolver(dnResolver)
    ldaptiveAuthenticator.setAuthenticationHandler(handler)
    // pac4j:
    val authenticator = new LdapProfileService(connectionFactory, ldaptiveAuthenticator, "dummy")
    authenticator.setAttributes("")
    authenticator.setUsernameAttribute(configuration.getString("pac4j.ldap.username_attribute").getOrElse("xxxx"))
    authenticator
  }

  override def configure(): Unit = {
    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])

    val authenticatorConf = configuration.getString("pac4j.authenticator").getOrElse("ldap")

    val authenticator = authenticatorConf match {
      case "ldap" => getLdapAuthenticator
      case "test" => new SimpleTestUsernamePasswordAuthenticator
      case _ => getLdapAuthenticator
    }

    val directBasicAuthClient = new DirectBasicAuthClient(authenticator)

    val secret = configuration.getString("pac4j.jwt_secret").fold[String](throw new Exception("missing secret"))(identity)

    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(secret))

    val parameterClient = new HeaderClient("Authorization", "Bearer", jwtAuthenticator)

    val config = new Config(new Clients(directBasicAuthClient, parameterClient))

    config.setHttpActionAdapter(new DefaultHttpActionAdapter())

    bind(classOf[Config]).toInstance(config)
    bind(classOf[JwtAuthenticator]).toInstance(jwtAuthenticator)
  }
}