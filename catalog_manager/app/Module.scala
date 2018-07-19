import com.google.inject.{ AbstractModule, Singleton }
import it.gov.daf.catalogmanager.listeners.{ IngestionListener, IngestionListenerImpl }
import it.gov.daf.common.sso.common.{ CacheWrapper, LoginClient }
import it.gov.daf.common.sso.client.LoginClientRemote
import org.slf4j.LoggerFactory
import play.api.{ Configuration, Environment }

@Singleton
class Module (environment: Environment, configuration: Configuration) extends AbstractModule {

  private val logger = LoggerFactory.getLogger("it.gov.daf.catalogmanager.Module")

  def configure() = {

    logger.info { "executing module.." }

    // REMEMBER TO LEAVE COMMENT FOR DEALING WIth Ingestion of file
    bind(classOf[IngestionListener]).to(classOf[IngestionListenerImpl]).asEagerSingleton()

    val cacheWrapper = new CacheWrapper(Option(30L), Option(0L))// cookie 30 min, credential not needed
    bind(classOf[CacheWrapper]).toInstance(cacheWrapper)

    val securityManHost: Option[String] = configuration.getString("security.manager.host")
    require(securityManHost.nonEmpty,"security.manager.host entry not provided")

    val loginClientRemote = new LoginClientRemote(securityManHost.get)
    bind(classOf[LoginClientRemote]).toInstance(loginClientRemote)
    bind(classOf[LoginClient]).to(classOf[LoginClientRemote])// for the initialization of SecuredInvocationManager
    //private val secInvokManager = SecuredInvocationManager.init( LoginClientRemote.init(SEC_MANAGER_HOST) )

  }

}
