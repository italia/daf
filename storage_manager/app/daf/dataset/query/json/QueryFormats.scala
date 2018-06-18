package daf.dataset.query.json

import daf.dataset.query._
import daf.web.json.JsonReadsSyntax
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads

object QueryFormats {

  private val havingWithoutGroupBy = ValidationError { "Query with [having] clause is missing [groupBy]" }

  val reader: Reads[Query] = for {
    select  <- SelectClauseFormats.reader.optional
    where   <- WhereClauseFormats.reader.optional
    groupBy <- GroupByClauseFormats.reader.optional
    having  <- HavingClauseFormats.reader.optional.filter(havingWithoutGroupBy) { _ => groupBy.isDefined }
    limit   <- LimitClauseFormats.reader.optional
  } yield Query(
    select  = select getOrElse SelectClause.*,
    where   = where,
    groupBy = groupBy,
    having  = having,
    limit   = limit
  )

}
