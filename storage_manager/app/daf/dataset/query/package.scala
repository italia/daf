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

package daf.dataset

package object query {

  implicit class ColumnOps(column: Column) {

    def as(alias: String): Column = column match {
      case _: AliasColumn => column
      case _              => AliasColumn(column, alias)
    }

    def asOpt(alias: Option[String]): Column = alias.map { as } getOrElse column

    def >(otherColumn: Column): ComparisonOperator   = Gt(column, otherColumn)
    def >=(otherColumn: Column): ComparisonOperator  = Gte(column, otherColumn)
    def <(otherColumn: Column): ComparisonOperator   = Lt(column, otherColumn)
    def <=(otherColumn: Column): ComparisonOperator  = Lte(column, otherColumn)
    def ===(otherColumn: Column): ComparisonOperator = Eq(column, otherColumn)
    def =!=(otherColumn: Column): ComparisonOperator = Neq(column, otherColumn)

  }

  implicit class FilterOperatorOps(operator: FilterOperator) {

    def and(otherOperator: FilterOperator) = (operator, otherOperator) match {
      case (and1: And, and2: And) => and1 ++ and2.filters
      case (and1: And, _)         => and1 ++ Seq(otherOperator)
      case (_        , and2: And) => And { operator +: and2.filters }
      case (_        , _)         => And { Seq(operator, otherOperator) }
    }

    def or(otherOperator: FilterOperator) = (operator, otherOperator) match {
      case (or1: Or, or2: Or) => or1 ++ or2.filters
      case (or1: Or, _)       => or1 ++ Seq(otherOperator)
      case (_      , or2: Or) => Or { operator +: or2.filters }
      case (_      , _)       => Or { Seq(operator, otherOperator) }
    }

  }

  def not(filterOperator: FilterOperator): FilterOperator = Not(filterOperator)

}
