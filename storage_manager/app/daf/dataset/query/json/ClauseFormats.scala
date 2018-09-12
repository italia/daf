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
import play.api.libs.json._

object SelectClauseFormats {

  val reader: Reads[SelectClause] = (__ \ "select").read[JsArray].map { jsArray =>
    SelectClause {
      jsArray.value.map { _.as[Column](ColumnFormats.reader) }
    }
  }

}

object WhereClauseFormats {

  val reader: Reads[WhereClause] = (__ \ "where").read[FilterOperator](FilterFormats.reader).map { WhereClause }

}

object HavingClauseFormats {

  val reader: Reads[HavingClause] = (__ \ "having").read[FilterOperator](FilterFormats.reader).map { HavingClause }

}

object GroupByClauseFormats {

  val reader: Reads[GroupByClause] = (__ \ "groupBy").read[JsArray].map { jsArray =>
    GroupByClause {
      jsArray.value.map { _.as[Column](SimpleColumnFormats.readerNamed) }
    }
  }

}

object JoinClauseFormats {

  private def read(key: String)(f: (Reference, FilterOperator) => JoinClause): Reads[JoinClause] = (__ \ key).read[JsObject].andThen {
    for {
      on  <- (__ \ "on").read[FilterOperator] { FilterFormats.reader }
      ref <- ReferenceFormats.reader
    } yield f(ref, on)
  }

  private val leftJoinReader  = read("left")  { LeftJoinClause  }
  private val rightJoinReader = read("right") { RightJoinClause }
  private val outerJoinReader = read("outer") { OuterJoinClause }
  private val innerJoinReader = read("inner") { InnerJoinClause }

  private val joinReader = innerJoinReader orElse leftJoinReader orElse outerJoinReader orElse rightJoinReader orElse {
    Reads { jsValue =>
      JsError { s"Invalid join [$jsValue]: must be one of [left, right, outer, inner]" }
    }
  }

  val reader = (__ \ "join").read[JsArray].map {
    _.value.map { _.as[JoinClause](joinReader) }
  }

}

object LimitClauseFormats {

  val reader: Reads[LimitClause] = (__ \ "limit").read[JsNumber] andThen Reads.IntReads map { LimitClause }

}

object ClauseFormats {

  val select  = SelectClauseFormats.reader

  val where   = WhereClauseFormats.reader

  val having  = HavingClauseFormats.reader

  val groupBy = GroupByClauseFormats.reader

  val join    = JoinClauseFormats.reader

  val limit   = LimitClauseFormats.reader

}

