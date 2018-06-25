package daf.dataset.query.jdbc

import cats.effect.IO
import cats.syntax.traverse.toTraverseOps
import cats.syntax.monad.catsSyntaxMonad
import cats.instances.list.catsStdInstancesForList
import cats.instances.stream.catsStdInstancesForStream
import daf.dataset.query.Query
import doobie._
import doobie.implicits._

import scala.util.Try

class JdbcQueryService(transactor: Transactor[IO]) {

  private def exec(fragment: Fragment) = fragment.execWith {
    HPS.executeQuery { JdbcQueryOps.result }
  }

  def exec(query: Query, table: String): Try[JdbcResult] = Writers.sql(query, table).write.map { exec(_).transact(transactor).unsafeRunSync }

}

object JdbcQueryOps {

  val header: ResultSetIO[Header] = FRS.getMetaData.map { metadata =>
    1 to metadata.getColumnCount map { metadata.getCatalogName }
  }

  val genericRow: ResultSetIO[Row] = FRS.getMetaData.flatMap { metadata =>
    List.range(1, metadata.getColumnCount).traverse[ResultSetIO, AnyRef] { FRS.getObject }
  }

  val genericStream: ResultSetIO[Stream[Row]] = genericRow.whileM[Stream] { HRS.next }

  val result: ResultSetIO[JdbcResult] = for {
    h      <- header
    stream <- genericStream
  } yield JdbcResult(h, stream)

}
