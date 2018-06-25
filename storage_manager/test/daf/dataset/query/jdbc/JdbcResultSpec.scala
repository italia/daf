package daf.dataset.query.jdbc

import java.sql.Timestamp
import java.time.{ LocalDateTime, OffsetDateTime, ZoneOffset }
import java.util.TimeZone

import org.scalatest.{ MustMatchers, WordSpec }
import play.api.libs.json.{ JsBoolean, JsNumber, JsObject, JsString }

class JdbcResultSpec extends WordSpec with MustMatchers {

  "A JDBC Result container" must {

    "convert to CSV" in {
      JdbcResults.flat.toCsv.toList must be {
        List(
          """"int", "string", "bool", "timestamp"""",
          """1, "str1", true, "2018-06-25T09:00:00Z"""",
          """2, "str2", false, "2018-06-25T09:30:00Z""""
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
    rows   = Stream(
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
      )
    )
  )

}
