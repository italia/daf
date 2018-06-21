package daf.dataset.query.jdbc

import daf.dataset.query._
import org.scalatest.{ MustMatchers, WordSpec }
import doobie.implicits.toSqlInterpolator

import scala.annotation.tailrec
import scala.util.Success

class WhereFragmentSpec extends WordSpec with MustMatchers {

  "A where fragment writer" must {

    "serialize a where clause in SQL" in {
      WhereFragment.writer { WhereClauses.simple }.run.map { _._1.toString } must be {
        Success { fr"WHERE NOT(((col1 > col2 AND col1 <> 'string') OR col1 <> col3))".toString }
      }
    }

    "serialize a very long select without stack overflow" in {
      WhereFragment.writer { WhereClauses.nested }.run must be { 'Success }
    }

  }

}

object WhereClauses {

  val simple = WhereClause {
    not {
      { NamedColumn("col1") >   NamedColumn("col2")   } and
      { NamedColumn("col1") =!= ValueColumn("string") } or
      { NamedColumn("col1") =!= NamedColumn("col3")   }
    }
  }

  @tailrec
  private def nest(op: FilterOperator, n: Int = 10000): FilterOperator = if (n == 0) op else nest(not(op), n - 1)

  val nested = WhereClause {
    nest { ValueColumn(true) =!= ValueColumn(false) }
  }

}