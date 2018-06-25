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