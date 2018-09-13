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

import cats.instances.try_.catsStdInstancesForTry
import cats.instances.list.catsStdInstancesForList
import cats.data.WriterT.catsDataApplicativeForWriterT
import cats.syntax.traverse.toTraverseOps
import daf.dataset.query.Query
import doobie.util.fragment.Fragment

/**
  * Groups all different `Writer` generators under one roof.
  */
object Writers {

  def select(query: Query): QueryFragmentWriter[ColumnReference] = ColumnFragments.select { query.select }

  def where(query: Query): QueryFragmentWriter[Unit] = query.where.map { FilterFragments.where } getOrElse QueryFragmentWriter.unit

  def join(query: Query, tableRef: Map[String, String]): QueryFragmentWriter[List[Unit]] = query.join.getOrElse { Seq.empty }.toList.zipWithIndex.traverse[QueryFragmentWriter, Unit] { case (clause, index) =>
    FilterFragments.join(clause, s"JT${index + 1}", tableRef)
  }

  def union(query: Query, tableRef: Map[String, String]): QueryFragmentWriter[List[Unit]] = query.union.getOrElse { Seq.empty }.toList.traverse[QueryFragmentWriter, Unit] {
    FilterFragments.union(_, tableRef)
  }

  def from(table: String): QueryFragmentWriter[Unit] = TableFragments.from(table)

  def groupBy(query: Query, reference: ColumnReference): QueryFragmentWriter[Unit] = query.groupBy.map { GroupingFragments.groupBy(_, reference) } getOrElse QueryFragmentWriter.unit

  def having(query: Query): QueryFragmentWriter[Unit] = query.having.map { FilterFragments.having } getOrElse QueryFragmentWriter.unit

  def limit(query: Query, defaultLimit: Option[Int]): QueryFragmentWriter[Unit] =
    query.limit.map  { RowFragments.limit(_, defaultLimit) } orElse
    defaultLimit.map { RowFragments.limit(_, None) } getOrElse
    QueryFragmentWriter.unit

  def sql(query: Query, table: String, tableReference: Map[String, String], defaultLimit: Option[Int] = None): QueryFragmentWriter[Unit] = for {
    reference <- select(query)
    _         <- from(table)
    _         <- join(query, tableReference)
    _         <- where(query)
    _         <- union(query, tableReference)
    _         <- groupBy(query, reference)
    _         <- having(query)
    _         <- limit(query, defaultLimit)
  } yield ()

  def explain(query: Query, table: String, tableReference: Map[String, String], defaultLimit: Option[Int] = None): QueryFragmentWriter[Unit] = for {
    _ <- QueryFragmentWriter.tell { Fragment.const("EXPLAIN") }
    _ <- sql(query, table, tableReference, defaultLimit)
  } yield ()

}