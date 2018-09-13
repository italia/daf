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
import daf.web.json.JsonReadsSyntax
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads

object QueryFormats {

  private val havingWithoutGroupBy = ValidationError { "Query with [having] clause is missing [groupBy]" }

  implicit val reader: Reads[Query] = for {
    select  <- SelectClauseFormats.reader.optional("select")
    where   <- WhereClauseFormats.reader.optional("where")
    join    <- JoinClauseFormats.reader.optional("join")
    union   <- UnionClauseFormats.reader.optional("union")
    groupBy <- GroupByClauseFormats.reader.optional("groupBy")
    having  <- HavingClauseFormats.reader.optional("having").filterNot(havingWithoutGroupBy) { clause => clause.nonEmpty && groupBy.isEmpty }
    limit   <- LimitClauseFormats.reader.optional("limit")
  } yield Query(
    select  = select getOrElse SelectClause.*,
    where   = where,
    join    = join,
    union   = union,
    groupBy = groupBy,
    having  = having,
    limit   = limit
  )

}
