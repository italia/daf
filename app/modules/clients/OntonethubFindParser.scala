package modules.clients

import com.fasterxml.jackson.databind.ObjectMapper

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.lang.Float

case class Result(id: String, label: String, score: Float) {
  override def toString = s"${id}, ${label}, ${score}"
}

/**
 * 	NOTE: currently disabled
 *  this is a facility object for parsing results.
 *  TODO: check if we need to re-module the JSON model result here 
 */
object OntonethubFindParser {

  val json_mapper = new ObjectMapper
  val json_reader = json_mapper.reader

  def parse(data: String) = {
    json_reader.readTree(data)
      .get("results").toList
      .map { item =>

        val id = item.get("id")
          .textValue().replaceAll("^.*/(.*)$", "$1")
        val label = item.get("http://www.w3.org/2000/01/rdf-schema#label")
          .toList.head.get("value").textValue()
        val score = item.get("http://stanbol.apache.org/ontology/entityhub/query#score")
          .toList.head.get("value").toString()

        Result(id, label, Float.valueOf(score))
      }
      .sortBy(_.score)(Ordering[Float].reverse)
  }

}
