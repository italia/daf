package controllers

import akka.actor.ActorSystem

import org.apache.hadoop.security.UserGroupInformation
import org.apache.spark.sql.{ DataFrame, SparkSession }

import org.pac4j.play.store.PlaySessionStore

import play.api.Configuration
import play.api.mvc.{ AnyContent, Request, Result }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Random, Success, Try }

class TestController(val configuration: Configuration,
                            override val playSessionStore: PlaySessionStore,
                            val sparkSession: SparkSession,
                            system: ActorSystem) extends AbstractController(configuration, playSessionStore) {

  implicit lazy val executionContext = system.dispatchers.lookup("akka.actor.test-dispatcher")

  type GenericAction[A]   = Request[AnyContent] => A
  type ResultAction       = GenericAction[Result]
  type FutureAction       = GenericAction[Future[Result]]

  private def executeQuery[A](f: DataFrame => A) = f {
    sparkSession.sqlContext.createDataFrame {
      Stream.continually { ProductData.random }.take { Random.nextInt(10000) + 10000 }
    }
  }

  private def runValidated(expectedUser: String)(f: String => Unit) = for {
    validatedUser <- validateUser(expectedUser)
    _             <- Try { f(validatedUser) }
    _             <- validateUser(expectedUser)
  } yield Ok

  private def validateUser(expectedUserName: String): Try[String] =
    if (expectedUserName == currentUser.name) Success { expectedUserName }
    else Failure {
      new RuntimeException(s"User information doesn't match thread context on thread [${Thread.currentThread.getName}]: expected [$expectedUserName] but found [${currentUser.name}]")
    }
  def currentUser = UserInformation.fromHadoop { UserGroupInformation.getCurrentUser }

  def withRandomUser[A](action: String => GenericAction[A]): GenericAction[A] = action { s"user-${Random.nextInt(5000)}" }

  def impersonatedAction(action: String => ResultAction): ResultAction = withRandomUser { userId =>
    doImpersonation { action(userId) } apply userId
  }

  def futureImpersonatedAction(action: String => ResultAction): FutureAction =  withRandomUser { userId => request =>
    Future {
      doImpersonation { action(userId) } apply userId apply request
    }
  }

  private def syncCall(f: String => Unit) = impersonatedAction { impersonatedUser => _ =>
    runValidated(impersonatedUser)(f).get
  }

  private def asyncCall(f: String => Unit) = futureImpersonatedAction { impersonatedUser => _ =>
    runValidated(impersonatedUser)(f).get
  }

  // arbitrary test calls

  def userCall = syncCall { _ => Thread.sleep(Random.nextInt(100) + 500l) }

  def sparkCall = syncCall { _ =>
    executeQuery {
      _.select("name", "price")
        .where("price < 50")
        .count()
    }
  }

  def futureCall = asyncCall { impersonatedUser =>
    Await.result(
      Future { validateUser(impersonatedUser).get }, // will throw the exception and fail the future if validation fails
      3.seconds
    )
  }

}

sealed case class UserInformation(name: String, groups: Seq[String])

object UserInformation {

  def fromHadoop(hadoopData: UserGroupInformation) = apply(hadoopData.getUserName, Seq.empty)

  def fromContext = fromHadoop { UserGroupInformation.getCurrentUser  }

}

sealed case class ProductData(id: Int, name: String, price: Double)

object ProductData {

  def random = apply(
    Random.nextInt(10000) + 5000,
    s"product_${Random.nextInt(1000)}",
    { Random.nextDouble() * 100 } + 10
  )

}
