package daf.dataset.query.json

import daf.dataset.query._
import daf.web.json.JsonReadsSyntax
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads

object QueryFormats {

  private val havingWithoutGroupBy = ValidationError { "Query with [having] clause is missing [groupBy]" }

  val reader: Reads[Query] = for {
    select  <- SelectClauseFormats.reader.optional("select")
    where   <- WhereClauseFormats.reader.optional("where")
    groupBy <- GroupByClauseFormats.reader.optional("groupBy")
    having  <- HavingClauseFormats.reader.optional("having").filterNot(havingWithoutGroupBy) { clause => clause.nonEmpty && groupBy.isEmpty }
    limit   <- LimitClauseFormats.reader.optional("limit")
  } yield Query(
    select  = select getOrElse SelectClause.*,
    where   = where,
    groupBy = groupBy,
    having  = having,
    limit   = limit
  )

}
