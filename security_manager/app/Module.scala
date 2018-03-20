import com.google.inject.{AbstractModule, Singleton}
import it.gov.daf.common.sso.common.{CacheWrapper, LoginClient}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.sso.LoginClientLocal
import play.api.{Configuration, Environment}
import scala.sys.process._

@Singleton
class Module(environment: Environment, configuration: Configuration) extends AbstractModule{


  def configure(): Unit ={

    println("executing module..")

    println("--env--")
    println("ls -l".!! )
    println("-------")
    println("ls -l conf".!! )
    println("-------")
    println("ls -l conf/mnt".!! )
    println("-------")

    bind(classOf[LoginClient]).to(classOf[LoginClientLocal])//for the initialization of SecuredInvocationManager

    //val cookieExpiration = configuration.getLong("cookie.expiration")
    //val tokenExpiration = configuration.getLong("token.expiration")
    val cookieExpiration =  Option(ConfigReader.cookieExpiration.toLong)
    val tokenExpiration = Option(ConfigReader.tokenExpiration.toLong)

    val cacheWrapper = new CacheWrapper(cookieExpiration,tokenExpiration)
    bind(classOf[CacheWrapper]).toInstance(cacheWrapper)

  }

}


