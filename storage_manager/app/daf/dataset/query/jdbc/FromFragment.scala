package daf.dataset.query.jdbc

import doobie.implicits.toSqlInterpolator

object FromFragment {

  def writer(tableName: String): QueryFragmentWriter[Unit] = QueryFragmentWriter.tell { fr"FROM $tableName" }

}