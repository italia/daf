package controllers

import java.net.URI

import com.google.inject.Inject
import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import org.apache.spark.sql.SparkSession
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.collection.convert.decorateAsScala._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.While",
    "org.wartremover.warts.DefaultArguments",
    "org.wartremover.warts.ImplicitParameter",
    "org.wartremover.warts.NonUnitStatements"
  )
)
@Api("physical-dataset")
class PhysicalDatasetController @Inject()(configuration: Configuration) extends Controller {

  val sparkSession: SparkSession = SparkSession.builder().master("local").getOrCreate()

  @ApiOperation(value = "given a physical dataset URI it returns a json document with the first 'limit' number of rows", produces = "application/json")
  def getDataset(@ApiParam(value = "the dataset's physical URI") uri: String,
                 @ApiParam(value = "the dataset's format") format: String,
                 @ApiParam(value = "max number of rows to return") limit: Int = configuration.getInt("max_number_of_rows").fold[Int](1000)(identity)) = Action {
    val datasetURI = new URI(uri)
    val locationURI = new URI(datasetURI.getSchemeSpecificPart)
    val locationScheme = locationURI.getScheme
    val actualFormat = format match {
      case "avro" => "com.databricks.spark.avro"
      case format: String => format
    }
    locationScheme match {
      case "hdfs" =>
        val location = locationURI.getSchemeSpecificPart
        val df = sparkSession.read.format(actualFormat).load(location)
        val doc = s"[${df.takeAsList(limit).asScala.map(row => {Utility.rowToJson(df.schema)(row)}).mkString(",")}]"
        Ok(doc)
      case _ =>
        Ok(Json.toJson(""))
    }
  }
}
