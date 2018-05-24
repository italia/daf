package controllers

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

import com.google.inject.Inject

import controllers.modules.TestAbstractModule

import it.teamdigitale.instances._

import org.scalatest._
import org.pac4j.play.store.PlaySessionStore

class AbstractControllerSpec extends TestAbstractModule
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ConfigurationInstance
  with AkkaInstance
  with SparkSessionInstance {

  @Inject protected var sessionStoreInstance: PlaySessionStore = _

  implicit lazy val executionContext = system.dispatchers.lookup("akka.actor.test-dispatcher")

  private def withController[U](f: TestController => U) = f { new TestController(configuration, sessionStore, sparkSession, system) }

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

    "performing spark jobs" must {

      "not share user information across concurrent threads" in withController { controller =>
        Await.result(
          Future.sequence { spark(controller) },
          30.seconds
        )
      }
    }

    "performing complex asynchronous operations" must {

      "share user information across concurrent threads" in withController { controller =>
        a [RuntimeException] should be thrownBy Await.result(
          Future.sequence { async(controller) },
          30.seconds
        )
      }
    }
  }

}


