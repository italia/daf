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

package daf.dataset.query.jdbc

import cats.syntax.traverse.toTraverseOps
import cats.syntax.monad.catsSyntaxMonad
import cats.instances.list.catsStdInstancesForList
import cats.instances.stream.catsStdInstancesForStream
import config.ImpalaConfig
import daf.dataset.query.Query
import daf.instances.TransactorInstance
import doobie._
import doobie.implicits._

import scala.util.Try

/**
  * Given a `Transactor` instance, this class serves as a bridge for execution of [[Query]] data.
  * @param impalaConfig the configuration for impala JDBC client
  */
class JdbcQueryService(protected val impalaConfig: ImpalaConfig) { this: TransactorInstance =>

  private def exec(fragment: Fragment) = fragment.execWith {
    HPS.executeQuery { JdbcQueryOps.result }
  }

  /**
    * Executes the [[Query]] on the given table name (usually inclusive of the database name), impersonating the given
    * `userId`.
    * @param query the [[Query]] to execute
    * @param table the table on which to run the query, generally including the database name
    * @param userId the id of the user on behalf of whom the query should be executed
    * @return
    */
  def exec(query: Query, table: String, userId: String): Try[JdbcResult] = Writers.sql(query, table).write.map {
    exec(_).transact { transactor(userId) }.unsafeRunSync
  }

}

/**
  * Internal object that provides `ResultIO` instance to facilitate composition of doobie actions.
  */
object JdbcQueryOps {

  val header: ResultSetIO[Header] = FRS.getMetaData.map { metadata =>
    1 to metadata.getColumnCount map { metadata.getColumnName }
  }

  val genericRow: ResultSetIO[Row] = FRS.getMetaData.flatMap { metadata =>
    List.range(1, metadata.getColumnCount + 1).traverse[ResultSetIO, AnyRef] { FRS.getObject }
  }

  val genericStream: ResultSetIO[Stream[Row]] = genericRow.whileM[Stream] { HRS.next }

  val result: ResultSetIO[JdbcResult] = for {
    h      <- header
    stream <- genericStream
  } yield JdbcResult(h, stream)

}
