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

package daf.dataset.query

/**
  * Internal representation of a query that can be translated by lower layers and passed to various execution engines.
  */
case class Query(select: SelectClause,
                 where: Option[WhereClause],
                 groupBy: Option[GroupByClause],
                 having: Option[HavingClause],
                 limit: Option[LimitClause]) {

  def hasLimit = limit.isDefined

  def hasGroupBy = groupBy.isDefined

  def hasWhere = where.isDefined

  def hasHaving = having.isDefined

  def hasFilters = hasWhere || hasHaving

}

sealed trait Clause

// Group By

case class GroupByClause(columns: Seq[Column]) extends Clause

// Select

case class SelectClause(columns: Seq[Column]) extends Clause

object SelectClause {

  def * = apply { Seq(WildcardColumn) }

}

// Where

case class WhereClause(filter: FilterOperator) extends Clause

// Having

case class HavingClause(filter: FilterOperator) extends Clause

// Limit

case class LimitClause(limit: Int) extends Clause

// Operators

sealed trait FilterOperator

sealed trait ComparisonOperator extends FilterOperator

case class Gt(left: Column, right: Column) extends ComparisonOperator
case class Gte(left: Column, right: Column) extends ComparisonOperator
case class Lt(left: Column, right: Column) extends ComparisonOperator
case class Lte(left: Column, right: Column) extends ComparisonOperator
case class Eq(left: Column, right: Column) extends ComparisonOperator
case class Neq(left: Column, right: Column) extends ComparisonOperator

sealed trait LogicalOperator extends FilterOperator

case class And(filters: Seq[FilterOperator]) extends LogicalOperator {

  def ++(otherFilters: Seq[FilterOperator]) = this.copy(
    this.filters ++ otherFilters
  )

}

case class Or(filters: Seq[FilterOperator]) extends LogicalOperator {
  def ++(otherFilters: Seq[FilterOperator]) = this.copy(
    this.filters ++ otherFilters
  )
}

case class Not(filter: FilterOperator) extends LogicalOperator

// Columns

sealed trait Column

sealed trait FunctionColumn extends Column

sealed trait AggregationColumn extends FunctionColumn {
  def column: Column
}

case class Max(column: Column) extends AggregationColumn
case class Min(column: Column) extends AggregationColumn
case class Avg(column: Column) extends AggregationColumn
case class Count(column: Column) extends AggregationColumn
case class Sum(column: Column) extends AggregationColumn

case class NamedColumn(name: String) extends Column

case class ValueColumn(value: Any) extends Column

case object WildcardColumn extends Column

case class AliasColumn(column: Column, alias: String) extends Column