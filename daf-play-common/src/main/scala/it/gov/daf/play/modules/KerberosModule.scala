package it.gov.daf.play.modules

import java.util.concurrent.Executors
import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Cancellable}
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.AbstractModule
import org.slf4j.LoggerFactory
import play.api.{Configuration, Environment}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.sys.process.{Process, ProcessLogger}

@Singleton
class KerberosModule @Inject() (
  system: ActorSystem,
  environment: Environment,
  conf: Configuration,
  lifecycle: ApplicationLifecycle
) extends AbstractModule {

  private val keyTab = conf.getString("kerberos.keytab").get
  private val principal = conf.getString("kerberos.principal").get

  require(keyTab.nonEmpty, "set kerberos.keytab in your application.conf")
  require(principal.nonEmpty, "set kerberos.principal in your application.conf")

  //this configuration is defined in the reference.conf
  private val refreshInterval: FiniteDuration = conf
    .getMilliseconds("kerberos.keytab_refresh_interval")
    .get milliseconds

  private val threadExec = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
    .setNameFormat("kerberos-%d")
    .setDaemon(true)
    .build())
  private implicit val ec = ExecutionContext.fromExecutor(threadExec)

  private val log = LoggerFactory.getLogger(this.getClass.getName)
  log.debug(s"init ${this.getClass.getName} module from daf-play-modules")

  //this is used to be thread local
  @volatile private var cancellable: Cancellable = _

  private val kInitCmd = Process(s"/usr/bin/kinit -kt $keyTab $principal")

  override def configure(): Unit =
    cancellable = system.scheduler.schedule(0 seconds, refreshInterval) { kInitRefresh() }

  def kInitRefresh(): Unit = {
    log.info("start refreshing kerberos credentials")
    val result = kInitCmd.!(ProcessLogger(
      info => log.debug(info),
      err => log.error(err)
    ))
    if (result == 0) log.info(s"refreshed kerberos credentials for $keyTab")
    else log.error(s"error refreshing credentials for $keyTab")
  }

  lifecycle.addStopHook { () =>
    Future {
      cancellable.cancel()
      threadExec.shutdown()
    }
  }
}
