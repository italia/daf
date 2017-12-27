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

package daf.dataset

import play.api.libs.json.Json

case class Query(
  select: Option[List[String]],
  where: Option[List[String]],
  groupBy: Option[GroupBy],
  limit: Option[Int]
)

case class GroupBy(
  groupColumn: String,
  conditions: List[GroupCondition]
)

case class GroupCondition(
  column: String,
  aggregationFunction: String
)

object json {

  implicit val groupConditionWrites = Json.writes[GroupCondition]
  implicit val groupConditionReads = Json.reads[GroupCondition]

  implicit val groupByWrites = Json.writes[GroupBy]
  implicit val groupByReads = Json.reads[GroupBy]

  implicit val queryWrites = Json.writes[Query]
  implicit val queryReads = Json.reads[Query]

}
