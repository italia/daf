package daf.dataset.query.jdbc

import doobie.implicits.toSqlInterpolator

object TableFragments {

  def from(tableName: String): QueryFragmentWriter[Unit] = QueryFragmentWriter.tell { fr"FROM $tableName" }

}