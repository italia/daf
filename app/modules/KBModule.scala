package modules

import javax.inject._

import play.api.inject.ApplicationLifecycle
import play.api.mvc._

import scala.concurrent.Future
import com.google.inject.ImplementedBy
import play.api.Play
import play.api.Application
import play.api.Environment
import play.api.Configuration
import scala.concurrent.ExecutionContext
import play.api.Logger
import it.almawave.linkeddata.kb.utils.ConfigHelper
import it.almawave.linkeddata.kb.repo._
import scala.concurrent.ExecutionContext.Implicits.global
import java.nio.file.Paths
import play.api.Mode
import java.io.File
import it.almawave.linkeddata.kb.repo.RDFRepository
import com.typesafe.config.ConfigFactory

@ImplementedBy(classOf[KBModuleBase])
trait KBModule

@Singleton
class KBModuleBase @Inject() (lifecycle: ApplicationLifecycle) extends KBModule {

  // TODO: SPI per dev / prod
  val kbrepo = RDFRepository.memory()

  // when application starts...
  @Inject
  def onStart(
    app: Application,
    env: Environment,
    configuration: Configuration)(implicit ec: ExecutionContext) {

    val kbconf = configuration.getConfig("kb")
      .getOrElse(Configuration.empty)
      .underlying

    kbrepo.configuration(kbconf)
    val conf = kbrepo.configuration()

    Logger.info("KBModule.START....")
    Logger.debug("KBModule using configuration:\n" + ConfigHelper.pretty(conf))

    println("KBModule using configuration:\n" + ConfigHelper.pretty(conf))

    // this is needed for ensure proper connection(s) etc
    kbrepo.start()

    // reset prefixes to default ones
    kbrepo.prefixes.clear()
    kbrepo.prefixes.add(kbrepo.prefixes.DEFAULT.toList: _*)

    if (conf.hasPath("cache"))
      kbrepo.io.importFrom(conf.getString("cache"))

    // CHECK the initial (total) triples count
    var triples = kbrepo.store.size()

    Logger.info(s"KBModule> ${triples} triples loaded")

  }

  // when application stops...
  lifecycle.addStopHook({ () =>

    Future.successful {

      // this is useful for saving files, closing connections, release indexes, etc
      kbrepo.stop()
      Logger.info("KBModule.STOP....")

    }

  })

}

