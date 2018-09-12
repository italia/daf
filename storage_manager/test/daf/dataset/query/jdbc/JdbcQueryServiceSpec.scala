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

import cats.effect.IO
import cats.instances.list.catsStdInstancesForList
import daf.dataset.query.{ Count, GroupByClause, Gt, NamedColumn, Query, SelectClause, ValueColumn, WhereClause }
import daf.instances.H2TransactorInstance
import doobie.free.KleisliInterpreter
import doobie.free.connection.AsyncConnectionIO
import doobie.implicits.{ toConnectionIOOps, toSqlInterpolator }
import doobie.util.query.Query0
import doobie.util.transactor.{ Strategy, Transactor }
import doobie.util.update.Update
import org.apache.commons.dbcp.BasicDataSource
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpec }

class JdbcQueryServiceSpec extends WordSpec with MustMatchers with BeforeAndAfterAll {

  private lazy val service = new JdbcQueryService(null, None) with H2TransactorInstance

  override def beforeAll(): Unit = JdbcQueries.prepare.transact { service.transactor("") }.unsafeRunSync() match {
    case (_     , rows) if rows == 0   => throw new RuntimeException("Unable to start test: [rows] were not created")
    case (_, _)                        => // do nothing
  }

  "A jdbc query service" must {

    "run queries" in  {
      service.exec(JdbcQueries.select, "user", Map.empty, "").map { _.toCsv.toList }.get must be {
        List(
          """"COUNTRY", "COUNTS"""",
          """"Italy", 2""",
          """"Netherlands", 1"""
        )
      }
    }
  }
}

object JdbcQueries {

  type User = (String, String, Int, String)

  private def createTransactor(dataSource: BasicDataSource) = Transactor[IO, BasicDataSource](
    dataSource, a => IO(a.getConnection), KleisliInterpreter[IO].ConnectionInterpreter, Strategy.void
  )

  val ddl =
    sql"""
      CREATE TABLE user(
        id VARCHAR,
        username VARCHAR,
        age SMALLINT,
        country VARCHAR
      )
    """.update.run

  Query0.apply("").stream

  val insert =
    Update[User]("INSERT INTO user(id, username, age, country) VALUES (?, ?, ?, ?)").updateMany[List] {
      List(
        ("id1", "user1", 42, "Italy"),
        ("id2", "user2", 32, "Italy"),
        ("id3", "user3", 27, "Italy"),
        ("id4", "user4", 33, "Netherlands")
      )
    }

  val prepare = for {
    table  <- JdbcQueries.ddl
    insert <- JdbcQueries.insert
  } yield (table, insert)

  val select = Query(
    select  = SelectClause {
      Seq(
        NamedColumn("country"), Count(NamedColumn("id")) as "counts"
      )
    },
    where   = Some {
      WhereClause { Gt(NamedColumn("age"), ValueColumn(30)) }
    },
    join    = None,
    groupBy = Some {
      GroupByClause { Seq(NamedColumn("country")) }
    },
    having  = None,
    limit   = None
  )

}
