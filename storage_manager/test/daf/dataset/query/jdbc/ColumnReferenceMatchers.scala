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
