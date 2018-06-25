package daf.dataset.query.jdbc

import daf.dataset.query._
import org.scalatest.{ MustMatchers, WordSpec }
import doobie.implicits.toSqlInterpolator

import scala.annotation.tailrec
import scala.util.Success

class SelectFragmentSpec extends WordSpec with MustMatchers {

  "A [select] fragment writer" must {

    "serialize a [select] clause in SQL" in {
      ColumnFragments.select { SelectClauses.simple }.run.map { _._1.toString } must be {
        Success { fr"SELECT col1, col2 AS alias1, 1, 'string' AS alias2, MAX(col3) AS alias3, SUM(true)".toString }
      }
    }

    "create a column/alias reference set" in {
      ColumnFragments.select { SelectClauses.simple }.run.get._2 must have (
        ColumnReferenceMatchers hasColumn "col1",
        ColumnReferenceMatchers hasColumn "col2",
        ColumnReferenceMatchers hasColumn "1",
        ColumnReferenceMatchers hasColumn "'string'",
        ColumnReferenceMatchers hasColumn "col3",
        ColumnReferenceMatchers hasColumn "true",
        ColumnReferenceMatchers hasAlias  "alias1",
        ColumnReferenceMatchers hasAlias  "alias2",
        ColumnReferenceMatchers hasAlias  "alias3"
      )
    }

    "serialize a very long [select] without stack overflow" in {
      ColumnFragments.select { SelectClauses.nested }.run must be { 'Success }
    }

  }

}

object SelectClauses {

  val simple = SelectClause {
    Seq(
      NamedColumn("col1"),
      NamedColumn("col2") as "alias1",
      ValueColumn(1),
      ValueColumn("string") as "alias2",
      Max(NamedColumn("col3")) as "alias3",
      Sum(ValueColumn(true))
    )
  }

  @tailrec
  private def nest(column: Column, n: Int = 10000): Column = if (n == 0) column else nest(Sum(column), n - 1)

  val nested = SelectClause {
    Seq { nest(ValueColumn(true)) }
  }

}