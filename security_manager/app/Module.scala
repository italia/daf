import com.google.inject.{AbstractModule, Singleton}
import it.gov.daf.common.sso.common.{CacheWrapper, LoginClient}
import it.gov.daf.sso.LoginClientLocal
import play.api.{Configuration, Environment}

@Singleton
class Module(environment: Environment, configuration: Configuration) extends AbstractModule{


  def configure(): Unit ={

    println("executing module..")
    bind(classOf[LoginClient]).to(classOf[LoginClientLocal])//for the initialization of SecuredInvocationManager

    val cookieExpiration = configuration.getLong("cookie.expiration")
    val tokenExpiration = configuration.getLong("token.expiration")

    val cacheWrapper = new CacheWrapper(cookieExpiration,tokenExpiration)
    bind(classOf[CacheWrapper]).toInstance(cacheWrapper)

  }

}


