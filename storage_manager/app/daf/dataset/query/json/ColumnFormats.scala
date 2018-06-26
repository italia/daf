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
import daf.web.json.CommonReads
import play.api.libs.json._

object SimpleColumnFormats {

  private val readAlias = (__ \ "alias").readNullable[String]

  private def readValue: Reads[Column] = Reads[Column] {
    case JsString(string)   => JsSuccess { ValueColumn(string) }
    case JsNumber(number)   => JsSuccess { ValueColumn(number) }
    case JsBoolean(boolean) => JsSuccess { ValueColumn(boolean) }
    case _: JsObject        => JsError { "Invalid value [obj] representation: must be a string, number or boolean" }
    case _: JsArray         => JsError { "Invalid value [array] representation: must be a string, number or boolean" }
    case unsupported        => JsError { s"Invalid value [$unsupported] representation: must be a string, number or boolean" }
  }

  private val readNamedColumn: Reads[Column] = (__ \ "name").read[String].map {
    case "*"     => WildcardColumn
    case colName => NamedColumn(colName)
  }
  private val readValueColumn: Reads[Column] = (__ \ "value").read[JsValue] andThen readValue

  val reader = for {
    alias  <- readAlias
    column <- readNamedColumn orElse readValueColumn
  } yield column asOpt alias

}

object AggregationColumnFormats {

  private def readColumn(name: String) = (__ \ name).read[JsValue] andThen SimpleColumnFormats.reader

  private val invalidAggregationReader = Reads[Column] { jsValue =>
    JsError { s"Invalid aggregation representation encountered - must be one of max, min, avg, count or sum: [$jsValue]" }
  }

  val reader: Reads[Column] =
    readColumn("max").map[Column]   { Max   } orElse
    readColumn("min").map[Column]   { Min   } orElse
    readColumn("avg").map[Column]   { Avg   } orElse
    readColumn("count").map[Column] { Count } orElse
    readColumn("sum").map[Column]   { Sum   } orElse
    invalidAggregationReader

}

object ColumnFormats {

  val reader: Reads[Column] = CommonReads.choice {
    case "name" | "value" => SimpleColumnFormats.reader
    case _                => AggregationColumnFormats.reader
  }

}
