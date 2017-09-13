
import play._

import javax.inject.Inject
import com.google.inject.{ AbstractModule, Singleton }
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future
import modules._

@Singleton
class Global @Inject() (lifecycle: ApplicationLifecycle) {

  @Inject
  def onStart() {
    Logger.info("#### Application STOP")
  }

  // REVIEW here
  lifecycle.addStopHook { () =>
    Future.successful({
      Logger.info("#### Application STOP")
    })
  }

  // TODO: plug a servicefactory for repository

}

@Singleton
class StartModule extends AbstractModule {

  def configure() = {

    Logger.info("\n\nCHECKING: StartModule.configure()")

    bind(classOf[KBModule]).to(classOf[KBModuleBase]).asEagerSingleton()
  }

}

