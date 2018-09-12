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
import it.gov.daf.common.utils._

import scala.util.Try

/**
  * Creates `Writer` instances that read filter data from queries and composes over the `Writer` into SQL fragments.
  */
object FilterFragments {

  private def writeColumn(column: Column): Trampoline[String] = column match {
    case ValueColumn(value: String)     => Free.pure { s"'${escape(value)}'" }
    case ValueColumn(value)             => Free.pure { value.toString }
    case NamedColumn(columnRegex(name)) => Free.pure { name }
    case _                              => recursionError[String] { new IllegalArgumentException("Invalid operand encountered: columns or constants only are allowed") }
  }

  private def _writeComparison(left: Column, right: Column)(f: (String, String) => String): Trampoline[String] = for {
    l <- writeColumn(left)
    r <- writeColumn(right)
  } yield f(l, r)

  private def writeComparisonOp(op: ComparisonOperator): Trampoline[String] = op match {
    case Gt(left, right)  => _writeComparison(left, right) { (l, r) => s"$l > $r" }
    case Gte(left, right) => _writeComparison(left, right) { (l, r) => s"$l >= $r" }
    case Lt(left, right)  => _writeComparison(left, right) { (l, r) => s"$l < $r" }
    case Lte(left, right) => _writeComparison(left, right) { (l, r) => s"$l <= $r" }
    case Eq(left, right)  => _writeComparison(left, right) { (l, r) => s"$l = $r" }
    case Neq(left, right) => _writeComparison(left, right) { (l, r) => s"$l <> $r" }
  }

  private def writeLogicalOp(op: LogicalOperator): Trampoline[String] = op match {
    case And(inner) => inner.toList.traverse[Trampoline, String] { writeFilterOp }.map { _.mkString("(", " AND ", ")") }
    case Or(inner)  => inner.toList.traverse[Trampoline, String] { writeFilterOp }.map { _.mkString("(", " OR ", ")") }
    case Not(inner) => writeFilterOp(inner).map { s => s"NOT($s)" }
  }

  private def writeFilterOp(op: FilterOperator): Trampoline[String] = op match {
    case logical: LogicalOperator       => Free.defer { writeLogicalOp(logical) }
    case comparison: ComparisonOperator => Free.defer { writeComparisonOp(comparison) }
  }

  private def writeReference(reference: Reference, tableRef: Map[String, String]) = reference match {
    case UnresolvedReference(uri)  => Free.liftF[Try, String] { tableRef.get(uri).~>[Try] }
    case ResolvedReference(target) => Free.pure[Try, String] { target }
  }

  private def writeJoinType(join: JoinClause) = join match {
    case LeftJoinClause(_, _)  => Free.pure[Try, String] { "LEFT JOIN"  }
    case InnerJoinClause(_, _) => Free.pure[Try, String] { "JOIN"       }
    case OuterJoinClause(_, _) => Free.pure[Try, String] { "OUTER JOIN" }
    case RightJoinClause(_, _) => Free.pure[Try, String] { "RIGHT JOIN" }
  }

  private def writeJoin(joinClause: JoinClause, tableRef: Map[String, String]) = for {
    join <- writeJoinType(joinClause)
    ref  <- writeReference(joinClause.reference, tableRef)
    cond <- writeFilterOp(joinClause.on)
  } yield s"$join $ref ON $cond"

  /**
    * Creates a [[QueryFragmentWriter]] for `WHERE` clauses in a query.
    */
  def where(whereClause: WhereClause) = QueryFragmentWriter.ask {
    writeFilterOp { whereClause.filter }.runTailRec.map { s => Fragment.const(s"WHERE $s") }
  }

  /**
    * Creates a [[QueryFragmentWriter]] for `HAVING` clauses in a query.
    */
  def having(havingClause: HavingClause) = QueryFragmentWriter.ask {
    writeFilterOp { havingClause.filter }.runTailRec.map { s => Fragment.const(s"HAVING $s") }
  }

  def join(joinClause: JoinClause, tableRef: Map[String, String]) = QueryFragmentWriter.ask {
    writeJoin(joinClause, tableRef).runTailRec.map { Fragment.const(_) }
  }

}
