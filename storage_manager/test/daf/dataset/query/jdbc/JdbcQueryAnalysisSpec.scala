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

import cats.syntax.foldable.toFoldableOps
import cats.instances.list.catsStdInstancesForList
import org.scalatest.{ MustMatchers, WordSpec }

class JdbcQueryAnalysisSpec extends WordSpec with MustMatchers {

  "JDBC Query Analysis" must {

    "interpret a simple query explanation" in {
      JdbcQueryAnalyses.simple.foldMap { JdbcQueryAnalysis.fromString } must have (
        JdbcQueryAnalysisMatchers.memoryReservation { 400d },
        JdbcQueryAnalysisMatchers.memoryEstimate    { 294.912d },
        JdbcQueryAnalysisMatchers.numSteps          { 9 }
      )
    }
  }
}

private object JdbcQueryAnalyses {

  val simple = List(
    "Per-Host Resource Reservation: Memory=400.00MB",
    "Per-Host Resource Estimates: Memory=0.288GB",
    "WARNING: The following tables are missing relevant table and/or column statistics.",
    "database.table",
    "",
    "PLAN-ROOT SINK",
    "|",
    "08:EXCHANGE [UNPARTITIONED]",
    "|",
    "07:AGGREGATE [FINALIZE]",
    "|  output: count:merge(*)",
    "|  group by: A.col1",
    "|",
    "06:EXCHANGE [HASH(A.col1)]",
    "|",
    "03:AGGREGATE [STREAMING]",
    "|  output: count(*)",
    "|  group by: A.col1",
    "|",
    "02:HASH JOIN [INNER JOIN, PARTITIONED]",
    "|  hash predicates: A.col2 = B.col3",
    "|  runtime filters: RF000 <- B.col3",
    "|",
    "|--05:EXCHANGE [HASH(B.col3)]",
    "|  |",
    "|  01:SCAN HDFS [database.table b]",
    "|     partitions=1/1 files=3 size=3.41KB",
    "|",
    "04:EXCHANGE [HASH(A.code_level_2)]",
    "|",
    "00:SCAN HDFS [database.table a]",
    "   partitions=1/1 files=3 size=3.41KB",
    "   runtime filters: RF000 -> A.code_level_2"
  )

}
