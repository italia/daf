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

import daf.dataset.query.LimitClause
import doobie.util.fragment.Fragment

/**
  * Creates `Writer` instances that transform row-affecting clauses and composes over the `Writer` into SQL fragments.
  */
object RowFragments {

  private def chooseLimit(value: Int, defaultLimit: Option[Int]) = defaultLimit.map { math.min(value, _) } getOrElse value

  /**
    * Creates a [[QueryFragmentWriter]] for `LIMIT` clauses in a query.
    */
  def limit(limitClause: LimitClause, defaultLimit: Option[Int]): QueryFragmentWriter[Unit] = QueryFragmentWriter.tell {
    Fragment.const { s"LIMIT ${chooseLimit(limitClause.limit, defaultLimit)}" }
  }

  /**
    * Creates a [[QueryFragmentWriter]] for `LIMIT` clauses in a query, starting from a raw value.
    */
  def limit(value: Int, defaultLimit: Option[Int]): QueryFragmentWriter[Unit] = limit(
    LimitClause(value),
    defaultLimit
  )

}
