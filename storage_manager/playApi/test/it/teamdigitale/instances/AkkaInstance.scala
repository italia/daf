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

package it.teamdigitale.instances

import akka.actor.ActorSystem

import scala.concurrent.{ Await, TimeoutException }
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

trait AkkaInstance { this: ConfigurationInstance =>

  protected val systemName = s"TestSystem_${System.currentTimeMillis}"

  protected val actorSystem = ActorSystem(systemName, configuration.underlying)

  def terminateAkka() = Try { Await.result(actorSystem.terminate(), 20.seconds) } match {
    case Success(_)                       => // nothing to do
    case Failure(error: TimeoutException) => throw error
    case Failure(error)                   => throw new RuntimeException("Unable to terminate actor system", error)
  }

  def startAkka(): Unit = {
    actorSystem.registerOnTermination { println("Actor System stopped") }
  }

}