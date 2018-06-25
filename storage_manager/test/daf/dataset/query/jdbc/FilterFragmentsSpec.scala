package daf.dataset.query.jdbc

import daf.dataset.query._
import org.scalatest.{ MustMatchers, WordSpec }
import doobie.implicits.toSqlInterpolator

import scala.annotation.tailrec
import scala.util.Success

class FilterFragmentsSpec extends WordSpec with MustMatchers {

  "A [where] fragment writer" must {

    "serialize a [where] clause in SQL" in {
      FilterFragments.where { FilterClauses.simpleWhere }.run.map { _._1.toString } must be {
        Success { fr"WHERE NOT(((col1 > col2 AND col1 <> 'string') OR col1 <> col3))".toString }
      }
    }

    "serialize a very long [where] without stack overflow" in {
      FilterFragments.where { FilterClauses.nestedWhere }.run must be { 'Success }
    }

  }

  "A [having] fragment writer" must {

    "serialize a [having] clause in SQL" in {
      FilterFragments.having { FilterClauses.simpleHaving }.run.map { _._1.toString } must be {
        Success { fr"HAVING NOT(((col1 > col2 AND col1 <> 'string') OR col1 <> col3))".toString }
      }
    }

    "serialize a very long [having] without stack overflow" in {
      FilterFragments.having { FilterClauses.nestedHaving }.run must be { 'Success }
    }

  }

}

object FilterClauses {

  val simple = not {
    { NamedColumn("col1") >   NamedColumn("col2")   } and
    { NamedColumn("col1") =!= ValueColumn("string") } or
    { NamedColumn("col1") =!= NamedColumn("col3")   }
  }

  @tailrec
  private def nest(op: FilterOperator, n: Int = 10000): FilterOperator = if (n == 0) op else nest(not(op), n - 1)

  val nested = nest { ValueColumn(true) =!= ValueColumn(false) }

  val simpleWhere = WhereClause { simple }
  val nestedWhere = WhereClause { nested }

  val simpleHaving = HavingClause { simple }
  val nestedHaving = HavingClause { nested }

}