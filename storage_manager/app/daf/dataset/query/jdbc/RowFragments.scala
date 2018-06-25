package daf.dataset.query.jdbc

import daf.dataset.query.LimitClause
import doobie.util.fragment.Fragment

object RowFragments {

  def limit(limitClause: LimitClause) = QueryFragmentWriter.tell {
    Fragment.const { s"LIMIT ${limitClause.limit}" }
  }

}
