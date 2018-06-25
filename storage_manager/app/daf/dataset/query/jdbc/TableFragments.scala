package daf.dataset.query.jdbc

import doobie.util.fragment.Fragment

object TableFragments {

  def from(tableName: String): QueryFragmentWriter[Unit] = QueryFragmentWriter.tell { Fragment.const(s"FROM $tableName") }

}