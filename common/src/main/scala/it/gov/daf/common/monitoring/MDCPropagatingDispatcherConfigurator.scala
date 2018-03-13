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

package it.gov.daf.common.monitoring

import java.util.concurrent.TimeUnit

import akka.dispatch._
import com.typesafe.config.Config
import org.slf4j.MDC

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Configurator for a MDC propagating dispatcher.
  *
  * To use it, configure play like this:
  * {{{
  * play {
  *   akka {
  *     actor {
  *       default-dispatcher = {
  *         type = "it.gov.daf.common.monitoring.MDCPropagatingDispatcherConfigurator"
  *       }
  *     }
  *   }
  * }
  * }}}
  *
  * Credits to James Roper for the [[https://github.com/jroper/thread-local-context-propagation/ initial implementation]]
  */
class MDCPropagatingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new MDCPropagatingDispatcher(
    this,
    config.getString("id"),
    config.getInt("throughput"),
    FiniteDuration(config.getDuration("throughput-deadline-time", TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS),
    configureExecutor(),
    FiniteDuration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}

/**
  * A MDC propagating dispatcher.
  *
  * This dispatcher propagates the MDC current request context if it's set when it's executed.
  */
class MDCPropagatingDispatcher(_configurator: MessageDispatcherConfigurator,
                               id: String,
                               throughput: Int,
                               throughputDeadlineTime: Duration,
                               executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
                               shutdownTimeout: FiniteDuration)
  extends Dispatcher(_configurator, id, throughput, throughputDeadlineTime, executorServiceFactoryProvider, shutdownTimeout ) {

  self =>

  override def prepare(): ExecutionContext = new ExecutionContext {
    // capture the MDC
    val mdcContext = MDC.getCopyOfContextMap
    //val parent = Thread.currentThread().getId

    def execute(r: Runnable) = self.execute(new Runnable {
      def run() = {
        // backup the callee MDC context
        val oldMDCContext = MDC.getCopyOfContextMap

        // Run the runnable with the captured context
        setContextMap(mdcContext)
        //println(s"setto ${Thread.currentThread().getId} - $mdcContext - from $parent")
        try {
          r.run()
        } finally {
          // restore the callee MDC context

          setContextMap(oldMDCContext)
          //println(s"ripristino ${Thread.currentThread().getId} - $oldMDCContext - from $parent")
        }
      }
    })
    def reportFailure(t: Throwable) = self.reportFailure(t)
  }

  private[this] def setContextMap(context: java.util.Map[String, String]):Unit = {
    if (context == null) {
      MDC.clear()
    } else {
      MDC.setContextMap(context)
    }
  }

}