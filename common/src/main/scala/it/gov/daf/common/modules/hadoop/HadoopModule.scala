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

/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License atSecurityModule
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File
import java.net.{URL, URLClassLoader}
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Singleton}
import play.Logger
import play.Logger.ALogger
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.sys.process.Process

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements"
  )
)
class SchedulingTask @Inject()(val system: ActorSystem, val configuration: Configuration) {
  implicit private val alogger: ALogger = Logger.of(this.getClass.getCanonicalName)

  alogger.info("Started the keytab refresher")
  system.scheduler.schedule(10 seconds, configuration.getInt("keytab_refresh_interval").getOrElse(12 * 60) minutes) {
    alogger.info("About to refresh the keytab")
    val process = Process(s"/usr/bin/kinit -kt ${configuration.getString("keytab").getOrElse("")} ${configuration.getString("principal").getOrElse("")}")
    val _ = process.!
    alogger.info("Refreshed the keytab")
  }
}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Overloading",
    "org.wartremover.warts.NonUnitStatements"
  )
)
@Singleton
class HadoopModule @Inject()(val environment: Environment, val configuration: Configuration) extends AbstractModule {
  
  //private val hadoopConfiguration = new org.apache.hadoop.conf.Configuration()

  private val process = Process(s"/usr/bin/kinit -kt ${configuration.getString("keytab").getOrElse("")} ${configuration.getString("principal").getOrElse("")}")
  private val _ = process.!

  def addPath(dir: String): Unit = {
    val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    method.setAccessible(true)
    method.invoke(Thread.currentThread().getContextClassLoader, new File(dir).toURI.toURL)
    ()
  }

  def configure(): Unit = {
    bind(classOf[SchedulingTask]).asEagerSingleton()
    configuration.getString("hadoop_conf_dir").foreach(addPath)
    configuration.getString("hbase_conf_dir").foreach(addPath)
  }
}