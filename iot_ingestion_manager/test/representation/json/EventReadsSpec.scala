package representation.json

import org.scalatest.{ MustMatchers, WordSpec }
import representation.{ Event, MetricEventType }

class EventReadsSpec extends WordSpec with MustMatchers with JsonParsing {

  "Event reader" when {

    "parsing a simple representation" must {

      "read a full representation" in {

        read(EventReadsCases.simpleFull) { EventReads.event } must be {
          Event(
            id         = "some-id",
            source     = "test-source",
            version    = Some(100),
            timestamp  = 1532423327223l,
            certainty  = Some(0.75),
            eventType  = MetricEventType,
            customType = Some("sensor"),
            comment    = Some("Test reading with moderate certainty"),
            body       = """{"int":1,"string":"two","double":0.975,"boolean":false}""",
            attributes = Map(
              "int"     -> 1,
              "double"  -> 0.975d,
              "boolean" -> false
            )
          )
        }
      }

      "read a minimal representation" in {

        read(EventReadsCases.simpleOptionals) { EventReads.event } must be {
          Event(
            id         = "some-id",
            source     = "test-source",
            version    = None,
            timestamp  = 1532423327223l,
            certainty  = None,
            eventType  = MetricEventType,
            customType = None,
            comment    = None,
            body       = """{"int":1,"string":"two","double":0.975,"boolean":false}""",
            attributes = Map.empty[String, Any]
          )
        }
      }
    }

    "parsing a complex nested representation" must {

      "read a representation without error" in {
        read(EventReadsCases.complex) { EventReads.event }.attributes.keys.size must be { 1 }
      }
    }
  }
}

private object EventReadsCases {

  private def nestAttributes(n: Int, json: String): String = if (n == 0) json else nestAttributes(n - 1, s"""{"attr$n": $json}""")

  val simpleFull =
    s"""
       |{
       |  "id": "some-id",
       |  "source": "test-source",
       |  "version": 100,
       |  "timestamp": 1532423327223,
       |  "certainty": 0.75,
       |  "eventType": "metric",
       |  "customType": "sensor",
       |  "comment": "Test reading with moderate certainty",
       |  "body": {
       |    "int": 1,
       |    "string": "two",
       |    "double": 0.975,
       |    "boolean": false
       |  },
       |  "attributes": {
       |    "int": 1,
       |    "double": 0.975,
       |    "boolean": false
       |  }
       |}
     """.stripMargin

  val simpleOptionals =
    s"""
       |{
       |  "id": "some-id",
       |  "source": "test-source",
       |  "timestamp": 1532423327223,
       |  "eventType": "metric",
       |  "body": {
       |    "int": 1,
       |    "string": "two",
       |    "double": 0.975,
       |    "boolean": false
       |  }
       |}
     """.stripMargin

  val complex =
    s"""
       |{
       |  "id": "some-id",
       |  "source": "test-source",
       |  "timestamp": 1532423327223,
       |  "eventType": "metric",
       |  "body": {
       |    "int": 1,
       |    "string": "two",
       |    "double": 0.975,
       |    "boolean": false
       |  },
       |  "attributes": ${nestAttributes(10000, "true")}
       |}
     """.stripMargin

}
