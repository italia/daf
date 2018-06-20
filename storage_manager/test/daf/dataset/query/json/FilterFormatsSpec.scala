package daf.dataset.query.json

import daf.dataset.query._
import org.scalatest.{ MustMatchers, WordSpec }
import play.api.libs.json.JsResultException

class FilterFormatsSpec extends WordSpec with MustMatchers with JsonParsing {

  "Filter reader" must {

    "read successfully" when {

      "reading greater than" in {
        read(FilterJson.gt) { FilterFormats.reader } must be { Gt(NamedColumn("col1"), NamedColumn("col2")) }
      }

      "reading greater than or equal to" in {
        read(FilterJson.gte) { FilterFormats.reader } must be { Gte(NamedColumn("col1"), ValueColumn(1)) }
      }

      "reading less than" in {
        read(FilterJson.lt) { FilterFormats.reader } must be { Lt(NamedColumn("col1"), ValueColumn(0.5)) }
      }

      "reading less than or equal to" in {
        read(FilterJson.lte) { FilterFormats.reader } must be { Lte(NamedColumn("col1"), ValueColumn(0)) }
      }

      "reading equal to" in {
        read(FilterJson.eq) { FilterFormats.reader } must be { Eq(NamedColumn("col1"), ValueColumn("string")) }
      }

      "reading not equal to" in {
        read(FilterJson.neq) { FilterFormats.reader } must be { Neq(NamedColumn("col1"), ValueColumn(true)) }
      }

      "reading and" in {
        read(FilterJson.and) { FilterFormats.reader } must be { Gt(NamedColumn("col1"), NamedColumn("col2")) and Eq(NamedColumn("col3"), ValueColumn(false)) }
      }

      "reading or" in {
        read(FilterJson.or) { FilterFormats.reader } must be { Gt(NamedColumn("col1"), NamedColumn("col2")) or Eq(NamedColumn("col3"), ValueColumn(false)) }
      }

      "reading not" in {
        read(FilterJson.not) { FilterFormats.reader } must be { Not(Gt(NamedColumn("col1"), NamedColumn("col2"))) }
      }

      "reading complex nested filters" in {
        read(FilterJson.complex) { FilterFormats.reader } must be {
          Not {
            { Gt(NamedColumn("col1"), NamedColumn("col2")) or Eq(NamedColumn("col3"), ValueColumn(false)) } and
            Neq(NamedColumn("col4"), ValueColumn("string"))
          }
        }
      }
    }


    "return an error" when {
      "reading a complex function operand" in {
        a[JsResultException] must be thrownBy read(FilterJson.complexFunctionOperand) { FilterFormats.reader }
      }

      "reading an unsupported operator" in {
        a[JsResultException] must be thrownBy read(FilterJson.unsupportedOperator) { FilterFormats.reader }
      }
    }

  }


}

private object FilterJson {

  val gt =
    """
      |{
      |  "gt": {
      |    "left": "col1",
      |    "right": "col2"
      |  }
      |}
    """.stripMargin

  val gte =
    """
      |{
      |  "gte": {
      |    "left": "col1",
      |    "right": 1
      |  }
      |}
    """.stripMargin

  val lt =
    """
      |{
      |  "lt": {
      |    "left": "col1",
      |    "right": 0.5
      |  }
      |}
    """.stripMargin

  val lte =
    """
      |{
      |  "lte": {
      |    "left": "col1",
      |    "right": 0
      |  }
      |}
    """.stripMargin

  val eq =
    """
      |{
      |  "eq": {
      |    "left": "col1",
      |    "right": "'string'"
      |  }
      |}
    """.stripMargin

  val neq =
    """
      |{
      |  "neq": {
      |    "left": "col1",
      |    "right": true
      |  }
      |}
    """.stripMargin

  val and =
    """
      |{
      |  "and": [{
      |    "gt": { "left": "col1", "right": "col2" }
      |  }, {
      |    "eq": { "left": "col3", "right": false }
      |  }]
      |}
    """.stripMargin

  val or =
    """
      |{
      |  "or": [{
      |    "gt": { "left": "col1", "right": "col2" }
      |  }, {
      |    "eq": { "left": "col3", "right": false }
      |  }]
      |}
    """.stripMargin

  val not =
    """
      |{
      |  "not": {
      |    "gt": { "left": "col1", "right": "col2" }
      |  }
      |}
    """.stripMargin

  val complex =
    """
      |{
      |  "not": {
      |    "and": [{
      |      "or": [{
      |        "gt": { "left": "col1", "right": "col2" }
      |      }, {
      |        "eq": { "left": "col3", "right": false }
      |      }]
      |    }, {
      |      "neq": { "left": "col4", "right": "'string'" }
      |    }]
      |  }
      |}
    """.stripMargin

  val complexFunctionOperand =
    """
      |{
      |  "gt": { "left": [1, 2, 4], "right": false }
      |}
    """.stripMargin

  val unsupportedOperator =
    """
      |{
      |  "like": { "left": "col1", "right": "'prefix%'" }
      |}
    """.stripMargin

}
