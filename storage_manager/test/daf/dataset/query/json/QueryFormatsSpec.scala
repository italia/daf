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

class QueryFormatsSpec extends WordSpec with MustMatchers with JsonParsing {

  "Query reader" must {

    "read successfully" when {

      "reading a simple select clause" in {
        read(QueryJson.select) { QueryFormats.reader } must be {
          Query(
            select = SelectClause { Seq(NamedColumn("col1") as "alias1", ValueColumn(1)) },
            where  = None,
            groupBy = None,
            having  = None,
            limit = None
          )
        }
      }

      "reading a where" in {
        read(QueryJson.where) { QueryFormats.reader } must be {
          Query(
            select = SelectClause { Seq(NamedColumn("col1") as "alias1") },
            where  = Some {
              WhereClause { Gt(NamedColumn("col1"), NamedColumn("col2")) }
            },
            groupBy = None,
            having  = None,
            limit = None
          )
        }
      }

      "reading a groupBy" in {
        read(QueryJson.groupBy) { QueryFormats.reader } must be {
          Query(
            select = SelectClause { Seq(NamedColumn("col1") as "alias1") },
            where  = Some {
              WhereClause { Gt(NamedColumn("col1"), NamedColumn("col2")) }
            },
            groupBy = Some {
              GroupByClause { Seq(NamedColumn("col1") as "alias1") }
            },
            having  = None,
            limit = None
          )
        }
      }

      "reading a having" in {
        read(QueryJson.having) { QueryFormats.reader } must be {
          Query(
            select = SelectClause { Seq(NamedColumn("col1") as "alias1") },
            where  = Some {
              WhereClause { Gt(NamedColumn("col1"), NamedColumn("col2")) }
            },
            groupBy = Some {
              GroupByClause { Seq(NamedColumn("col1") as "alias1") }
            },
            having  = Some {
              HavingClause { Gt(NamedColumn("col1"), NamedColumn("col2")) }
            },
            limit = None
          )
        }
      }

      "reading a limit" in {
        read(QueryJson.limit) { QueryFormats.reader } must be {
          Query(
            select = SelectClause { Seq(NamedColumn("col1") as "alias1") },
            where  = Some {
              WhereClause { Gt(NamedColumn("col1"), NamedColumn("col2")) }
            },
            groupBy = Some {
              GroupByClause { Seq(NamedColumn("col1") as "alias1") }
            },
            having  = Some {
              HavingClause { Gt(NamedColumn("col1"), NamedColumn("col2")) }
            },
            limit = Some { LimitClause(100) }
          )
        }
      }

      "reading a select wildcard" in {
        read(QueryJson.selectWildcard) { QueryFormats.reader } must be {
          Query(
            select = SelectClause.*,
            where  = Some {
              WhereClause { Gt(NamedColumn("col1"), NamedColumn("col2")) }
            },
            groupBy = None,
            having  = None,
            limit = None
          )
        }
      }
    }


    "return an error" when {
      "reading a query with a having clause but without a groupBy" in {
        a[JsResultException] must be thrownBy read(QueryJson.havingNoGroupBy) { QueryFormats.reader }
      }
    }

  }

}

private object QueryJson {

  val select =
    """
      |{
      |  "select": [{
      |    "name": "col1", "alias": "alias1"
      |  }, {
      |    "value": 1
      |  }]
      |}
    """.stripMargin

  val where =
    """
      |{
      |  "select": [{
      |    "name": "col1", "alias": "alias1"
      |  }],
      |  "where": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  }
      |}
    """.stripMargin

  val groupBy =
    """
      |{
      |  "select": [{
      |    "name": "col1", "alias": "alias1"
      |  }],
      |  "where": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  },
      |  "groupBy": [{
      |    "name": "col1", "alias": "alias1"
      |  }]
      |}
    """.stripMargin

  val having =
    """
      |{
      |  "select": [{
      |    "name": "col1", "alias": "alias1"
      |  }],
      |  "where": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  },
      |  "groupBy": [{
      |    "name": "col1", "alias": "alias1"
      |  }],
      |  "having": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  }
      |}
    """.stripMargin

  val limit =
    """
      |{
      |  "select": [{
      |    "name": "col1", "alias": "alias1"
      |  }],
      |  "where": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  },
      |  "groupBy": [{
      |    "name": "col1", "alias": "alias1"
      |  }],
      |  "having": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  },
      |  "limit": 100
      |}
    """.stripMargin

  val selectWildcard =
    """
      |{
      |  "where": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  }
      |}
    """.stripMargin

  val havingNoGroupBy =
    """
      |{
      |  "select": [{
      |    "name": "col1", "alias": "alias1"
      |  }],
      |  "where": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  },
      |  "having": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  },
      |  "limit": 100
      |}
    """.stripMargin

}
