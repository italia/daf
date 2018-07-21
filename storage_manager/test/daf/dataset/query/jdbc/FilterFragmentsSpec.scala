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

import daf.dataset.query._
import org.scalatest.{ MustMatchers, WordSpec }
import doobie.implicits.toSqlInterpolator

import scala.annotation.tailrec
import scala.util.Success

class FilterFragmentsSpec extends WordSpec with MustMatchers {

  "A [where] fragment writer" must {

    "serialize a [where] clause in SQL" in {
      FilterFragments.where { FilterClauses.simpleWhere }.run.map { _._1.toString } must be {
        Success { fr"WHERE NOT(((col1 > col2 AND col1 <> 'string') OR col1 <> col3))".toString }
      }
    }

    "serialize a very long [where] without stack overflow" in {
      FilterFragments.where { FilterClauses.nestedWhere }.run must be { 'Success }
    }

  }

  "A [having] fragment writer" must {

    "serialize a [having] clause in SQL" in {
      FilterFragments.having { FilterClauses.simpleHaving }.run.map { _._1.toString } must be {
        Success { fr"HAVING NOT(((col1 > col2 AND col1 <> 'string') OR col1 <> col3))".toString }
      }
    }

    "serialize a very long [having] without stack overflow" in {
      FilterFragments.having { FilterClauses.nestedHaving }.run must be { 'Success }
    }

  }

}

object FilterClauses {

  val simple = not {
    { NamedColumn("col1") >   NamedColumn("col2")   } and
    { NamedColumn("col1") =!= ValueColumn("string") } or
    { NamedColumn("col1") =!= NamedColumn("col3")   }
  }

  @tailrec
  private def nest(op: FilterOperator, n: Int = 10000): FilterOperator = if (n == 0) op else nest(not(op), n - 1)

  val nested = nest { ValueColumn(true) =!= ValueColumn(false) }

  val simpleWhere = WhereClause { simple }
  val nestedWhere = WhereClause { nested }

  val simpleHaving = HavingClause { simple }
  val nestedHaving = HavingClause { nested }

}