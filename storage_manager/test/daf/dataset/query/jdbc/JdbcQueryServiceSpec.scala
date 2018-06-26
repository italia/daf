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
import com.cloudera.impala.jdbc41.{ DataSource => ImpalaDataSource }
import daf.dataset.query.{ Count, GroupByClause, Gt, NamedColumn, Query, SelectClause, ValueColumn, WhereClause }
import doobie.implicits.{ toConnectionIOOps, toSqlInterpolator }
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import org.apache.commons.dbcp.BasicDataSource
import org.apache.hive.jdbc.HiveDriver
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpec }

import scala.util.Success

class JdbcQueryServiceSpec extends WordSpec with MustMatchers with BeforeAndAfterAll {

  private lazy val service = new JdbcQueryService(JdbcQueries.transactor)

//  override def beforeAll(): Unit = JdbcQueries.prepare.transact { JdbcQueries.transactor }.unsafeRunSync() match {
//    case (_     , rows) if rows == 0   => throw new RuntimeException("Unable to start test: [rows] were not created")
//    case (_, _)                        => // do nothing
//  }

  "A jdbc query service" must {

    "run queries" in {
      service.exec(JdbcQueries.select, "user").map { _.toCsv.toList } must be {
        Success {
          List(
            """"COUNTRY", "COUNTS"""",
            """"Italy", 2""",
            """"Netherlands", 1"""
          )
        }
      }
    }
  }

}

object JdbcQueries {

  type User = (String, String, Int, String)

  private def configureDatasource(dataSource: BasicDataSource = new BasicDataSource) = {
    dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver")
    dataSource.setUrl("jdbc:h2:mem:")
    dataSource
  }

  private def configureImpala(dataSource: ImpalaDataSource = new ImpalaDataSource) = {
    dataSource.setURL(
      "jdbc:impala://slave1.novalocal:21050/;" +
        "AuthMech=1;" +
        "KrbRealm=PLATFORM.DAF.LOCAL;" +
        "KrbHostFQDN=slave1.novalocal;" +
        "KrbServiceName=impala;" +
        "SSL=1;" +
        "SSLKeyStore=/Library/Java/JavaVirtualMachines/jdk1.8.0_172.jdk/Contents/Home/jre/lib/security/jssecacerts;" +
        "SSLKeyStorePwd=changeitagain;" +
        "userGSSCredential=daf@DAF.GOV.IT"
    )
    dataSource
  }

  lazy val transactor = Transactor.fromDataSource[IO] { configureImpala() }

  val ddl =
    sql"""
      CREATE TABLE user(
        id VARCHAR,
        username VARCHAR,
        age SMALLINT,
        country VARCHAR
      )
    """.update.run

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
    groupBy = Some {
      GroupByClause { Seq(NamedColumn("country")) }
    },
    having = None,
    limit  = None
  )

}
