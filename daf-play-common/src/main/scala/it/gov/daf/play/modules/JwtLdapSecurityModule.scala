package it.gov.daf.play.modules

import java.time.Duration
import javax.inject.{Inject, Singleton}

import com.google.inject.AbstractModule
import org.ldaptive._
import org.ldaptive.auth.{Authenticator, BindAuthenticationHandler, SearchDnResolver}
import org.ldaptive.pool._
import org.ldaptive.ssl.SslConfig
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.http.client.direct.{DirectBasicAuthClient, HeaderClient}
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.ldap.profile.service.LdapProfileService
import org.pac4j.play.http.DefaultHttpActionAdapter
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import play.api.{ Configuration, Environment }

import scala.language.postfixOps

@Singleton
class JwtLdapSecurityModule @Inject() (environment: Environment, conf: Configuration) extends AbstractModule {
  private val authMethod = conf.getString("pac4j.authenticator")
  private val jwtSecret = conf.getString("pac4j.jwt_secret").get

  private val connTimeout = Duration.ofMillis(conf.getMilliseconds("pac4j.ldap.connect_timeout").get)
  private val respTimeout = Duration.ofMillis(conf.getMilliseconds("pac4j.ldap.response_timeout").get)
  private val ldapUrl = conf.getString("pac4j.ldap.url").get
  private val bindDn = conf.getString("pac4j.ldap.bind_dn").get
  private val bindPwd = conf.getString("pac4j.ldap.bind_pwd").get
  private val baseUserDn = conf.getString("pac4j.ldap.base_user_dn").get
  private val loginAttribute = conf.getString("pac4j.ldap.login_attribute").get
  private val usernameAttribute = conf.getString("pac4j.ldap.username_attribute").get

  //check for a valid auth method
  authMethod match {
    case Some("ldap") =>
      require(ldapUrl.nonEmpty, "set pac4j.ldap.url in your application.conf")
      require(bindDn.nonEmpty, "set pac4j.ldap.bind_dn in your application.conf")
      require(bindPwd.nonEmpty, "set pac4j.ldap.bind_pwd in your application.conf")
      require(baseUserDn.nonEmpty, "set pac4j.ldap.base_user_dn in your application.conf")
      require(loginAttribute.nonEmpty, "set pac4j.ldap.login_attribute in your application.conf")
      require(usernameAttribute.nonEmpty, "set pac4j.ldap.username_attribute in your application.conf")
      require(jwtSecret.nonEmpty, "set pac4j.jwt_secret in your application.conf")

    case Some("test") =>

    case other =>
      throw new IllegalArgumentException(s"No valid value for pa4j.authenticator provided $other")
  }


  override def configure() = {
    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])

    val authenticator = authMethod match {
      case Some("ldap") => ldapAuthenticator()
      case Some("test") => new SimpleTestUsernamePasswordAuthenticator
      case other =>
        throw new IllegalArgumentException(s"No valid value for pa4j.authenticator provided $other")
    }

    val directBasicAuthClient = new DirectBasicAuthClient(authenticator)

    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration(jwtSecret))

    val parameterClient = new HeaderClient("Authorization", "Bearer", jwtAuthenticator)

    val config = new Config(new Clients(directBasicAuthClient, parameterClient))
    config.setHttpActionAdapter(new DefaultHttpActionAdapter())

    bind(classOf[Config]).toInstance(config)
    bind(classOf[JwtAuthenticator]).toInstance(jwtAuthenticator)

  }

  private def ldapAuthenticator() = {
    val connConfig = new ConnectionConfig
    connConfig.setConnectTimeout(connTimeout)
    connConfig.setResponseTimeout(respTimeout)
    connConfig.setLdapUrl(ldapUrl)
    connConfig.setConnectionInitializer(new BindConnectionInitializer(bindDn, new Credential(bindPwd)))
    connConfig.setUseSSL(true)

    val sslConfig = new SslConfig()
    sslConfig.setTrustManagers() //TODO no more certificate validation, shall we keep it in this way?
    connConfig.setSslConfig(sslConfig)

    val connFactory = new DefaultConnectionFactory()
    connFactory.setConnectionConfig(connConfig)

    val poolConfig = new PoolConfig
    poolConfig.setMinPoolSize(1)
    poolConfig.setMaxPoolSize(2)
    poolConfig.setValidateOnCheckOut(true)
    poolConfig.setValidateOnCheckIn(true)
    poolConfig.setValidatePeriodically(false)

    val connectionPool = new BlockingConnectionPool
    connectionPool.setPoolConfig(poolConfig)
    connectionPool.setBlockWaitTime(Duration.ofMillis(1000))
    connectionPool.setValidator(new SearchValidator)
    connectionPool.setPruneStrategy(new IdlePruneStrategy)
    connectionPool.setConnectionFactory(connFactory)
    connectionPool.initialize()

    val pooledConnectionFactory = new PooledConnectionFactory
    pooledConnectionFactory.setConnectionPool(connectionPool)

    val handler = new BindAuthenticationHandler(new DefaultConnectionFactory(connConfig))

    val dnResolver = new SearchDnResolver(pooledConnectionFactory)
    dnResolver.setBaseDn(baseUserDn)
    dnResolver.setUserFilter(s"($loginAttribute={user})")

    val ldaptiveAuthenticator = new Authenticator
    ldaptiveAuthenticator.setDnResolver(dnResolver)
    ldaptiveAuthenticator.setAuthenticationHandler(handler)

    //FIXME dummy????
    val authenticator = new LdapProfileService(connFactory, ldaptiveAuthenticator, "dummy")
    authenticator.setAttributes("")
    authenticator.setUsernameAttribute(usernameAttribute)
    authenticator
  }
}
