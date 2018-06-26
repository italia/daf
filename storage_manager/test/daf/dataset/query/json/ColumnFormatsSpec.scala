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

package daf.dataset.query.json

import daf.dataset.query._
import org.scalatest.{ MustMatchers, WordSpec }
import play.api.libs.json.JsResultException

class ColumnFormatsSpec extends WordSpec with MustMatchers with JsonParsing {

  "Column reader" must {

    "read successfully" when {

      "reading a named column" in {
        read(ColumnJson.namedColumn) { ColumnFormats.reader } must be { NamedColumn("col1") }
      }

      "reading a named column with alias" in {
        read(ColumnJson.namedColumnWithAlias) { ColumnFormats.reader } must be { NamedColumn("col1") as "alias1" }
      }

      "reading a value column" in {
        read(ColumnJson.valueColumn) { ColumnFormats.reader } must be { ValueColumn("value1") }
      }

      "reading a value column with alias" in {
        read(ColumnJson.valueColumnWithAlias) { ColumnFormats.reader } must be { ValueColumn(5) as "alias1" }
      }

      "reading a function column" in {
        read(ColumnJson.maxNamedColumn) { ColumnFormats.reader } must be { Max(NamedColumn("col1")) }
      }

      "reading a function column with alias" in {
        read(ColumnJson.sumNamedColumnWithAlias) { ColumnFormats.reader } must be { Sum(NamedColumn("col1") as "sum_col1") }
      }

      "reading a function on value with alias" in {
        read(ColumnJson.countValueColumnWithAlias) { ColumnFormats.reader } must be { Count(ValueColumn(0) as "count_*") }
      }

    }


    "return an error" when {

      "reading an obj value" in {
        a[JsResultException] should be thrownBy read(ColumnJson.valueObjColumn) { ColumnFormats.reader }
      }

      "reading an array value" in {
        a[JsResultException] should be thrownBy read(ColumnJson.valueArrayColumn) { ColumnFormats.reader }
      }

      "reading nested aggregations" in {
        a[JsResultException] should be thrownBy read(ColumnJson.nestedAggregation) { ColumnFormats.reader }
      }

    }

  }


}

private object ColumnJson {

  val namedColumn =
    """
      |{
      |  "name": "col1"
      |}
    """.stripMargin

  val namedColumnWithAlias =
    """
      |{
      |  "name": "col1",
      |  "alias": "alias1"
      |}
    """.stripMargin

  val valueColumn =
    """
      |{
      |  "value": "value1"
      |}
    """.stripMargin

  val valueColumnWithAlias =
    """
      |{
      |  "value": 5,
      |  "alias": "alias1"
      |}
    """.stripMargin

  val valueObjColumn =
    """
      |{
      |  "value": {
      |    "key": "value"
      |  }
      |}
    """.stripMargin

  val valueArrayColumn =
    """
      |{
      |  "value": [1, 2]
      |}
    """.stripMargin

  val maxNamedColumn =
      """
        |{
        |  "max": {
        |    "name": "col1"
        |  }
        |}
      """.stripMargin

  val sumNamedColumnWithAlias =
    """
      |{
      |  "sum": {
      |    "name": "col1",
      |    "alias": "sum_col1"
      |  }
      |}
    """.stripMargin

  val countValueColumnWithAlias =
    """
      |{
      |  "count": {
      |    "value": 0,
      |    "alias": "count_*"
      |  }
      |}
    """.stripMargin

  val nestedAggregation =
    """
      |{
      |  "count": {
      |    "sum": {
      |      "value": 1
      |    }
      |  }
      |}
    """.stripMargin

}
