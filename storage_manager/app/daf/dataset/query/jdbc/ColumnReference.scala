package daf.dataset.query.jdbc

import cats.Monoid

sealed case class ColumnReference(names: Set[String], aliases: Set[String]) {

  def addName(name: String) = this.copy(
    names = this.names + name
  )

  def addAlias(alias: String) = this.copy(
    aliases = this.aliases + alias
  )

  def contains(s: String) = names.contains(s) || aliases.contains(s)

}

private object ColumnReferenceInstances extends Monoid[ColumnReference] {

  def empty = ColumnReference(Set.empty[String], Set.empty[String])

  def combine(ref1: ColumnReference, ref2: ColumnReference) = ColumnReference(
    names   = ref1.names ++ ref2.names,
    aliases = ref1.aliases ++ ref2.aliases
  )

}
