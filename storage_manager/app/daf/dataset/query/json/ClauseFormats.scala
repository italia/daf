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
      jsArray.value.map { _.as[Column](SimpleColumnFormats.reader) }
    }
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

  val limit   = LimitClauseFormats.reader

}

