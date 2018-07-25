package daf.stream

import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import cats .instances.try_.catsStdInstancesForTry
import representation.StreamData

import scala.util.{ Failure, Success, Try }

trait PayloadValidator {

  def validate(payload: Map[String, Any], streamData: StreamData): Try[Map[String, Any]]

}

object NoPayloadValidator extends PayloadValidator {

  def validate(payload: Map[String, Any], streamData: StreamData) = Success { payload }

}

object AvroPayloadValidator extends PayloadValidator {

  private def convertFieldType(name: String, value: Any) = value match {
    case _: String  => Success { "string"  }
    case _: Int     => Success { "int"     }
    case _: Long    => Success { "long"    }
    case _: Double  => Success { "double"  }
    case _: Boolean => Success { "boolean" }
    case _          => Failure { new IllegalArgumentException(s"Unsupported value type in payload [${value.getClass.getSimpleName}] for key [$name]") }
  }

  private def validateFieldType(name: String, fieldType: String, schema: Map[String, String]) = schema.get(name) match {
    case Some(schemaType) if fieldType == schemaType => Success { fieldType }
    case None                                        => Failure { new IllegalArgumentException(s"Field [$name] not found in schema") }
  }

  private def validateField(name: String, value: Any, schema: Map[String, String]) = for {
    fieldType <- convertFieldType(name, value)
    _         <- validateFieldType(name, fieldType, schema)
  } yield (name, value)

  def validate(payload: Map[String, Any], streamData: StreamData) = payload.toList.traverse[Try, (String, Any)] { case (name, value) => validateField(name, value, streamData.schema) }.map {
    _.toMap[String, Any]
  }

}