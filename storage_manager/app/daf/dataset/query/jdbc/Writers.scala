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
import daf.dataset.query.Query

object Writers {

  def select(query: Query): QueryFragmentWriter[ColumnReference] = ColumnFragments.select { query.select }

  def where(query: Query): QueryFragmentWriter[Unit] = query.where.map { FilterFragments.where } getOrElse QueryFragmentWriter.unit

  def from(table: String): QueryFragmentWriter[Unit] = TableFragments.from(table)

  def groupBy(query: Query, reference: ColumnReference): QueryFragmentWriter[Unit] = query.groupBy.map { GroupingFragments.groupBy(_, reference) } getOrElse QueryFragmentWriter.unit

  def having(query: Query): QueryFragmentWriter[Unit] = query.having.map { FilterFragments.having } getOrElse QueryFragmentWriter.unit

  def limit(query: Query): QueryFragmentWriter[Unit] = query.limit.map { RowFragments.limit } getOrElse QueryFragmentWriter.unit

  def sql(query: Query, table: String): QueryFragmentWriter[Unit] = for {
    reference <- select(query)
    _         <- from(table)
    _         <- where(query)
    _         <- groupBy(query, reference)
    _         <- having(query)
    _         <- limit(query)
  } yield ()

}