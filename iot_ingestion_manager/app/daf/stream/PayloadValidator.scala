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

package daf.stream

import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import cats.instances.try_.catsStdInstancesForTry
import play.api.libs.json._
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
    case _: Int     => Success { "int"     }
    case _: Long    => Success { "long"    }
    case _: Double  => Success { "double"  }
    case _: Boolean => Success { "boolean" }
    case _: String  => Success { "string"  }
    case _          => Failure { new IllegalArgumentException(s"Unsupported value type in payload [${value.getClass.getSimpleName}] for key [$name]") }
  }

  private def validateFieldType(name: String, fieldType: String, schema: Map[String, String]) = schema.get(name) match {
    case Some(schemaType) if fieldType == schemaType => Success { fieldType }
    case Some(schemaType)                            => Failure { new IllegalArgumentException(s"Incorrect type encountered for field [$name]: found [$fieldType] while expecting [$schemaType]") }
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

object AvroJsonPayloadValidator extends PayloadValidator {

  private def convertFieldType(name: String, value: Any) = value match {
    case JsNumber(i) if i.isValidInt  => Success { "int"     }
    case JsNumber(l) if l.isValidLong => Success { "long"    }
    case _: JsNumber                  => Success { "double"  }
    case _: JsBoolean                 => Success { "boolean" }
    case _: JsString                  => Success { "string"  }
    case _: JsArray                   => Success { "array"   }
    case _: JsObject                  => Success { "record"  }
    case _                            => Failure { new IllegalArgumentException(s"Unsupported value type in payload [${value.getClass.getSimpleName}] for key [$name]") }
  }

  private def validateFieldType(name: String, fieldType: String, schema: Map[String, String]) = schema.get(name) match {
    case Some(schemaType) if fieldType == schemaType => Success { fieldType }
    case Some("long")     if fieldType == "int"      => Success { fieldType } // handles widening
    case Some(schemaType)                            => Failure { new IllegalArgumentException(s"Incorrect type encountered for field [$name]: found [$fieldType] while expecting [$schemaType]") }
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