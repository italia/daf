package daf.dataset.query.json

import daf.dataset.query._
import play.api.libs.json._

object TableReferenceFormats {

  val reader: Reads[Reference] = (__ \ "table").read[String].map { TableReference }

}

object UriReferenceFormats {

  val reader: Reads[Reference] = (__ \ "uri").read[String].map { UriReference }

}

object ReferenceFormats {

  val reader: Reads[Reference] = UriReferenceFormats.reader

}
