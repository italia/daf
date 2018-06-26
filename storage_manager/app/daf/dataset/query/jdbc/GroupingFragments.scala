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
//      validated <- validateReference(columns.toSet, reference)
    } yield Fragment.const { s"GROUP BY ${columns mkString ", "}" }
  }

}
