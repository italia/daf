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

package controllers

import akka.stream.ActorMaterializer
import controllers.modules.TestAbstractModule
import daf.instances.{ AkkaInstance, ConfigurationInstance, SparkSessionInstance }
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class AbstractControllerSpec extends TestAbstractModule
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ConfigurationInstance
  with AkkaInstance
  with SparkSessionInstance {

  implicit lazy val executionContext = actorSystem.dispatchers.lookup("akka.actor.test-dispatcher")

  protected implicit lazy val materializer = ActorMaterializer.create { actorSystem }

  private def withController[U](f: TestController => U) = f { new TestController(configuration, sessionStore, sparkSession, actorSystem, executionContext) }

  override def beforeAll() = {
    startAkka()
  }

  private def parallelize[U](f: => U, numParallel: Int = 64)(implicit ec: ExecutionContext) = Stream.continually { Future(f) } take numParallel

  private def spark(controller: TestController, numParallel: Int = 64)(implicit ec: ExecutionContext) = parallelize(
    controller.sparkCall(null),
    numParallel
  )

  private def plain(controller: TestController, numParallel: Int = 64)(implicit ec: ExecutionContext) = parallelize(
    controller.userCall(null),
    numParallel
  )

  private def async(controller: TestController, numParallel: Int = 64)(implicit ec: ExecutionContext) = Stream.continually {
    controller.futureCall(null)
  } take numParallel

  "Abstract Controllers" when {

    "performing simple tasks" must {

      "not share user information across concurrent threads" in withController { controller =>
        Await.result(
          Future.sequence { plain(controller) },
          30.seconds
        )
      }
    }

//    "performing spark jobs" must {
//
//      "not share user information across concurrent threads" in withController { controller =>
//        Await.result(
//          Future.sequence { spark(controller) },
//          30.seconds
//        )
//      }
//    }
//
//    "performing complex asynchronous operations" must {
//
//      "share user information across concurrent threads" in withController { controller =>
//        a [RuntimeException] should be thrownBy Await.result(
//          Future.sequence { async(controller) },
//          30.seconds
//        )
//      }
//    }
  }

}


