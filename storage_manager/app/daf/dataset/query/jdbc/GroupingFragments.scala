package daf.dataset.query.jdbc

import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import cats.instances.try_.catsStdInstancesForTry
import daf.dataset.query.{ AliasColumn, Column, FunctionColumn, GroupByClause, NamedColumn }
import doobie.util.fragment.Fragment

import scala.util.{ Failure, Success, Try }

object GroupingFragments {

  private def validateColumns(columns: Seq[Column]) = columns.toList.traverse[Try, String] {
    case NamedColumn(name)   => Success { name }
    case column: AliasColumn => Failure { new IllegalArgumentException(s"Illegal alias column [${column.alias}] found in [groupBy]") }
    case _: FunctionColumn   => Failure { new IllegalArgumentException(s"Illegal function column found in [groupBy]") }
  }

  def validateReference(columns: Set[String], reference: ColumnReference) = reference.names.toList.traverse[Try, String] {
    case column if columns contains column => Success { column }
    case column                            => Failure { new IllegalArgumentException(s"Invalid column reference [$column] found; not in [groupBy]") }
  }

  def groupBy(groupByClause: GroupByClause, reference: ColumnReference) = QueryFragmentWriter.ask {
    for {
      columns   <- validateColumns(groupByClause.columns)
      validated <- validateReference(columns.toSet, reference)
    } yield Fragment.const { s"GROUP BY ${validated mkString ", "}" }
  }

}
