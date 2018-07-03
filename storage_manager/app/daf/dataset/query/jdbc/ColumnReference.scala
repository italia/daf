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

import cats.Monoid

/**
  * Basic container class for name and aliases as read from query clauses and various column types.
  * @note There will be an overlap of names and aliases, in the sense that a column that has an alias will be found in
  *       this reference by both its name '''and''' it alias.
  * @param names the `Set` of column names
  * @param aliases the `Set` of column aliases
  */
sealed case class ColumnReference(names: Set[String], aliases: Set[String]) {

  /**
    * Adds a column name to this reference
    */
  def addName(name: String) = this.copy(
    names = this.names + name
  )

  /**
    * Adds an alias to this reference
    */
  def addAlias(alias: String) = this.copy(
    aliases = this.aliases + alias
  )

  /**
    * Checks whether the given string is contained in this reference, either as a name or an alias.
    * @param s the name or alias to be checked
    */
  def contains(s: String) = names.contains(s) || aliases.contains(s)

}

private object ColumnReferenceInstances extends Monoid[ColumnReference] {

  def empty = ColumnReference(Set.empty[String], Set.empty[String])

  def combine(ref1: ColumnReference, ref2: ColumnReference) = ColumnReference(
    names   = ref1.names ++ ref2.names,
    aliases = ref1.aliases ++ ref2.aliases
  )

}
