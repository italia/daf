package daf.dataset.query.json

import daf.dataset.query._
import play.api.libs.json._

object ComparisonOperatorFormats {

  private val stringRegex = "'(.*)'".r

  private val readOperandJsValue: Reads[Column] = Reads[Column] {
    case JsString(stringRegex(value)) => JsSuccess { ValueColumn(value) }
    case JsString(columnName)         => JsSuccess { NamedColumn(columnName) }
    case JsNumber(number)             => JsSuccess { ValueColumn(number) }
    case JsBoolean(boolean)           => JsSuccess { ValueColumn(boolean) }
    case _: JsObject                  => JsError { "Invalid operand [obj] representation: must be a string, number, boolean or column name" }
    case _: JsArray                   => JsError { "Invalid operand [array] representation: must be a string, number, boolean or column name" }
    case unsupported                  => JsError { s"Invalid operand [$unsupported] representation: must be a string, number, boolean or column name" }
  }

  private val readOperands: Reads[(Column, Column)] = for {
    left  <- readOperand("left")
    right <- readOperand("right")
  } yield (left, right)

  private def readOperand(name: String): Reads[Column] = (__ \ name).read[JsValue] andThen readOperandJsValue

  private def readOperator(name: String)(f: (Column, Column) => FilterOperator): Reads[FilterOperator] =
    { (__ \ name).read[JsValue] andThen readOperands }.map { case (left, right) => f(left, right) }

  val reader: Reads[FilterOperator] =
    readOperator("gt")  { Gt  } orElse
    readOperator("gte") { Gte } orElse
    readOperator("lt")  { Lt  } orElse
    readOperator("lte") { Lte } orElse
    readOperator("eq")  { Eq  } orElse
    readOperator("neq") { Neq }

}

object LogicalOperators {

  private def readOperator: Reads[FilterOperator] = ComparisonOperatorFormats.reader orElse reader

  private def readNot: Reads[FilterOperator] = (__ \ "not").read[JsObject] andThen readOperator

  private def readAnd: Reads[FilterOperator] = (__ \ "and").read[JsArray].map { jsArray =>
    And {
      jsArray.value.map { _.as[FilterOperator](readOperator) }
    }
  }

  private def readOr: Reads[FilterOperator] = (__ \ "or").read[JsArray].map { jsArray =>
    Or {
      jsArray.value.map { _.as[FilterOperator](readOperator) }
    }
  }

  val reader: Reads[FilterOperator] = readNot orElse readAnd orElse readOr

}


object FilterFormats {

  val reader: Reads[FilterOperator] = ComparisonOperatorFormats.reader orElse LogicalOperators.reader

}
