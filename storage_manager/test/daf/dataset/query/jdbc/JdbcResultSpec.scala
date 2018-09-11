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

import java.sql.Timestamp
import java.time.{ LocalDateTime, OffsetDateTime }

import org.scalatest.{ MustMatchers, WordSpec }
import play.api.libs.json._

class JdbcResultSpec extends WordSpec with MustMatchers {

  "A JDBC Result container" must {

    "convert to CSV" in {
      JdbcResults.flat.toCsv.toList must be {
        List(
          """"int", "string", "bool", "timestamp"""",
          """1, "str1", true, "2018-06-25T09:00:00Z"""",
          """2, "str2", false, "2018-06-25T09:30:00Z"""",
          """<null>, <null>, false, <null>"""
        )
      }
    }

    "convert to json" in {
      JdbcResults.flat.toJson.toList must be {
        Seq(
          JsObject {
            Seq(
              "int"       -> JsNumber(1),
              "string"    -> JsString("str1"),
              "bool"      -> JsBoolean(true),
              "timestamp" -> JsString("2018-06-25T09:00:00Z")
            )
          },
          JsObject {
            Seq(
              "int"       -> JsNumber(2),
              "string"    -> JsString("str2"),
              "bool"      -> JsBoolean(false),
              "timestamp" -> JsString("2018-06-25T09:30:00Z")
            )
          },
          JsObject {
            Seq(
              "int"       -> JsNull,
              "string"    -> JsNull,
              "bool"      -> JsBoolean(false),
              "timestamp" -> JsNull
            )
          }
        )
      }
    }

  }

}

object JdbcResults {

  private val offset = OffsetDateTime.now().getOffset

  private def timestamp(dateTime: LocalDateTime) = Timestamp.from { dateTime.toInstant(offset) }

  val flat = JdbcResult(
    header = Seq("int", "string", "bool", "timestamp"),
    rows   = Vector(
      List(
        Int.box(1),
        "str1",
        Boolean.box(true),
        timestamp { LocalDateTime.of(2018, 6, 25, 9, 0) }
      ),
      List(
        Int.box(2),
        "str2",
        Boolean.box(false),
        timestamp { LocalDateTime.of(2018, 6, 25, 9, 30) }
      ),
      List(
        null,
        null,
        Boolean.box(false),
        null
      )
    )
  )

}
