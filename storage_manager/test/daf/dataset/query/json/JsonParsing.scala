package daf.dataset.query.json

import play.api.libs.json.{ Json, Reads }

trait JsonParsing {

  def read[A](json: String)(implicit reads: Reads[A]) = Json.parse(json).as[A]

}
