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

package it.gov.daf.common.modules.hadoop

import java.io.File
import java.net.{ URL, URLClassLoader }

import javax.inject.Inject
import akka.actor.ActorSystem
import com.google.inject.{ AbstractModule, Singleton }
import it.gov.daf.common.config.Read
import play.Logger
import play.Logger.ALogger
import play.api.{ Configuration, Environment }

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.sys.process.Process
import scala.util.{ Failure, Success, Try }

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Throw"
  )
)
class SchedulingTask @Inject()(val system: ActorSystem, val configuration: Configuration) {

  private val logger: ALogger = Logger.of(this.getClass.getCanonicalName)

  private implicit val executionContext = system.dispatcher

  private def prepareProcess = for {
    keytab    <- Read.string { "kerberos.keytab"    }.!
    principal <- Read.string { "kerberos.principal" }.!
  } yield Process(
    "/usr/bin/kinit",
    Seq("-kt", keytab, principal)
  )

  private def info(message: String) = Success { logger.info(message) }

  private def createSchedule() = for {
    process  <- prepareProcess
    interval <- Read.time { "kerberos.refresh_interval" } default 1.hour
  } yield system.scheduler.schedule(10.seconds, interval) {
    process.!
    logger.info { s"Refreshed keytab - next refresh due in [$interval]" }
  }

  createSchedule().read { configuration } match {
    case Success(_)     => logger.info { "Started scheduler - first refresh in [10 seconds]" }
    case Failure(error) => throw new RuntimeException("Unable to initialize ticket renewal mechanism", error)
  }

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Overloading",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Throw"
  )
)
@Singleton
class HadoopModule @Inject()(val environment: Environment, val configuration: Configuration) extends AbstractModule {

  private val logger: ALogger = Logger.of(this.getClass.getCanonicalName)

  private def prepareProcess = for {
    keytab    <- Read.string { "kerberos.keytab"    }.!
    principal <- Read.string { "kerberos.principal" }.!
  } yield Process(
    "/usr/bin/kinit",
    Seq("-kt", keytab, principal)
  )

  private def prepareClasspath = for {
    hadoopConf <- Read.string { "hadoop_conf_dir" } default "/etc/hadoop/conf"
    hbaseConf  <- Read.string { "hbase_conf_dir"  } default "/etc/hbase/conf"
  } yield addPaths(hadoopConf, hbaseConf)

  private def prepare = for {
    process <- prepareProcess.map { _.! }
    _       <- prepareClasspath
  } yield ()

  private def postBind() = prepare.read { configuration } match {
    case Success(_)     => logger.info { "Hadoop module started" }
    case Failure(error) => throw new RuntimeException("Unable to initialize Hadoop module", error)
  }

  def addPaths(dirs: String*): Unit = {
    val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    method.setAccessible(true)
    dirs.foreach { dir =>
      method.invoke(Thread.currentThread().getContextClassLoader, new File(dir).toURI.toURL)
    }
  }

  def configure(): Unit = {
    bind(classOf[SchedulingTask]).asEagerSingleton()
    postBind()
  }

}