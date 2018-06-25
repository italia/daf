package daf.dataset.query.jdbc

import daf.dataset.query.{ GroupByClause, Max, NamedColumn }
import doobie.implicits.toSqlInterpolator
import org.scalatest.{ MustMatchers, WordSpec }

import scala.util.Success

class GroupingFragmentsSpec extends WordSpec with MustMatchers {

  "A [groupBy] fragment writer" must {

    "serialize a [groupBy] clause in SQL" in {
      GroupingFragments.groupBy(GroupByClauses.valid, GroupByClauses.validRef).run.map { _._1.toString } must be {
        Success { fr"GROUP BY col1, col2".toString }
      }
    }

    "throw an error" when {
      "a [groupBy] clause contains an alias column" in {
        GroupingFragments.groupBy(GroupByClauses.invalidAlias, GroupByClauses.validRef).run must be { 'Failure }
      }

      "a [groupBy] clause contains an function column" in {
        GroupingFragments.groupBy(GroupByClauses.invalidFunction, GroupByClauses.validRef).run must be { 'Failure }
      }

      "an invalid column reference is encountered" in {
        GroupingFragments.groupBy(GroupByClauses.valid, GroupByClauses.invalidRef).run must be { 'Failure }
      }
    }

  }

}

object GroupByClauses {

  val validRef = ColumnReference(
    Set("col1", "col2"),
    Set("alias1")
  )

  val invalidRef = ColumnReference(
    Set("col1", "col2", "col3"),
    Set.empty[String]
  )

  val valid = GroupByClause {
    Seq(NamedColumn("col1"), NamedColumn("col2"))
  }

  val invalidAlias = GroupByClause {
    Seq(NamedColumn("col1") as "alias1")
  }

  val invalidFunction = GroupByClause {
    Seq(Max(NamedColumn("col1")))
  }

}