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
import cats.free.Free
import cats.free.Free.catsFreeMonadForFree
import daf.dataset.query._
import doobie.util.fragment.Fragment

import scala.annotation.tailrec
import scala.util.Try

/**
  * Creates `Writer` instances that read column data from queries and composes over the `Writer` into SQL fragments.
  */
object ColumnFragments {

  @tailrec
  private def buildReference(columns: List[Column], columnReference: ColumnReference = ColumnReferenceInstances.empty): ColumnReference = columns match {
    case WildcardColumn :: tail             => buildReference(tail, columnReference)
    case AliasColumn(column, alias) :: tail => buildReference(column :: tail, columnReference addAlias alias)
    case NamedColumn(name) :: tail          => buildReference(tail, columnReference addName name)
    case ValueColumn(value: String) :: tail => buildReference(tail, columnReference addName s"'${escape(value)}'")
    case ValueColumn(value) :: tail         => buildReference(tail, columnReference addName value.toString)
    case (agg: AggregationColumn) :: tail   => buildReference(agg.column :: tail, columnReference)
    case _ :: tail                          => buildReference(tail, columnReference)
    case Nil                                => columnReference
  }

  private def _writeColumn(column: Column)(f: String => String): Trampoline[String] = Free.defer[Try, String] { writeColumn(column) }.map { f }

  private def writeAggregation(aggregation: AggregationColumn): Trampoline[String] = aggregation match {
    case Max(inner)   => _writeColumn(inner) { col => s"MAX($col)" }
    case Min(inner)   => _writeColumn(inner) { col => s"MIN($col)" }
    case Avg(inner)   => _writeColumn(inner) { col => s"AVG($col)" }
    case Count(inner) => _writeColumn(inner) { col => s"COUNT($col)" }
    case Sum(inner)   => _writeColumn(inner) { col => s"SUM($col)" }
  }

  // Recursion is stackless in this case, meaning that it should never overflow the stack
  private def writeColumn(column: Column): Trampoline[String] = column match {
    case WildcardColumn                                     => Free.pure[Try, String] { "*" }
    case AliasColumn(inner, alias)                          => Free.defer { writeColumn(inner) }.map { col => s"$col AS $alias" }
    case NamedColumn(columnRegex(name))                     => Free.pure[Try, String] { name }
    case NamedColumn(qualifiedColumnRegex(qualifier, name)) => Free.pure[Try, String] { s"$qualifier.$name" }
    case ValueColumn(value: String)                         => Free.pure[Try, String] { s"'${escape(value)}'" }
    case ValueColumn(value)                                 => Free.pure[Try, String] { value.toString }
    case agg: AggregationColumn                             => writeAggregation(agg)
    case _                                                  => recursionError { new IllegalArgumentException("Invalid column type in [select] fragment") }
  }

  private def writeColumns(columns: List[Column]): Trampoline[List[String]] = columns.traverse[Trampoline, String] { writeColumn }

  private def buildFragment(columns: Seq[String]) = Try { Fragment.const(s"SELECT ${columns mkString ", "}") }

  /**
    * Creates a [[QueryFragmentWriter]] for `SELECT` clauses in a query.
    */
  def select(select: SelectClause) = QueryFragmentWriter[ColumnReference] {
    for {
      columns  <- writeColumns(select.columns.toList).runTailRec
      fragment <- buildFragment(columns)
    } yield fragment -> buildReference(select.columns.toList)
  }

}
