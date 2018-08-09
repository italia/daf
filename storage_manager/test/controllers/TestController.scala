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

import akka.actor.ActorSystem
import cats.Id
import daf.web.Impersonations
import it.gov.daf.common.web.WebAction
import org.apache.hadoop.security.UserGroupInformation
import org.apache.spark.sql.{ DataFrame, SparkSession }
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.api.mvc.{ AnyContent, Request, Result }

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Random, Success, Try }

class TestController(override val configuration: Configuration,
                     override val playSessionStore: PlaySessionStore,
                     val sparkSession: SparkSession,
                     protected implicit val system: ActorSystem,
                     protected implicit val executionContext: ExecutionContext) extends AbstractController(configuration, playSessionStore) {

  type GenericAction[A]   = Request[AnyContent] => A
  type ResultAction       = WebAction[Id, AnyContent]
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

  def impersonatedAction[A](action: String => WebAction[Id, AnyContent]): WebAction[Id, AnyContent] = withRandomUser { userId =>
    Impersonations.hadoop(proxyUser).wrap { action(userId) } apply userId
  }

  def futureImpersonatedAction(action: String => WebAction[Id, AnyContent]): FutureAction =  withRandomUser { userId => request =>
    Future {
      Impersonations.hadoop(proxyUser).wrap { action(userId) } apply userId apply request
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
