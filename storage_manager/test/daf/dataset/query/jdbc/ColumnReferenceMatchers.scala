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

import org.scalatest.matchers.{ HavePropertyMatchResult, HavePropertyMatcher }

object ColumnReferenceMatchers {

  def hasColumn(name: String) = new HavePropertyMatcher[ColumnReference, String] {
    def apply(colRef: ColumnReference) = HavePropertyMatchResult(
      colRef.names contains name,
      "names",
      name,
      colRef.names mkString ", "
    )
  }

  def hasAlias(alias: String) = new HavePropertyMatcher[ColumnReference, String] {
    def apply(colRef: ColumnReference) = HavePropertyMatchResult(
      colRef.aliases contains alias,
      "aliases",
      alias,
      colRef.aliases mkString ", "
    )
  }

}
