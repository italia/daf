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

package controllers

import java.net.URI

import com.databricks.spark.avro.SchemaConverters
import com.google.inject.Inject
import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import org.apache.avro.SchemaBuilder
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

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

  val sparkConfig = new SparkConf()
  sparkConfig.set("spark.driver.memory", configuration.getString("spark_driver_memory").getOrElse("128M"))
  val sparkSession: SparkSession = SparkSession.builder().master("local").config(sparkConfig).getOrCreate()

  @ApiOperation(value = "given a physical dataset URI it returns a json document with the first 'limit' number of rows", produces = "application/json, text/plain")
  def getDataset(@ApiParam(value = "the dataset's physical URI", required = true) uri: String,
                 @ApiParam(value = "the dataset's format", required = true) format: String,
                 @ApiParam(value = "max number of rows to return", required = false) limit: Option[Int]) = Action {
    val defaultLimit = configuration.getInt("max_number_of_rows").fold[Int](throw new Exception("it shouldn;'t happen"))(identity)
    val datasetURI = new URI(uri)
    val locationURI = new URI(datasetURI.getSchemeSpecificPart)
    val locationScheme = locationURI.getScheme
    val actualFormat = format match {
      case "avro" => "com.databricks.spark.avro"
      case format: String => format
    }
    locationScheme match {
      case "hdfs" if actualFormat == "text" =>
        val location = locationURI.getSchemeSpecificPart
        val rdd = sparkSession.sparkContext.textFile(location)
        val doc = rdd.take(limit.getOrElse(defaultLimit)).mkString("\n")
        Ok(doc).as("text/plain")
      case "hdfs" =>
        val location = locationURI.getSchemeSpecificPart
        val df = sparkSession.read.format(actualFormat).load(location)
        val doc = s"[${
          df.take(limit.getOrElse(defaultLimit)).map(row => {
            Utility.rowToJson(df.schema)(row)
          }).mkString(",")
        }]"
        Ok(doc).as(JSON)
      case _ =>
        Ok(Json.toJson(""))
    }
  }

  @ApiOperation(value = "given a physical dataset URI it returns its AVRO schema in json format", produces = "application/json, text/plain")
  def getDatasetSchema(@ApiParam(value = "the dataset's physical URI", required = true) uri: String,
                       @ApiParam(value = "the dataset's format", required = true) format: String) = Action {
    val datasetURI = new URI(uri)
    val locationURI = new URI(datasetURI.getSchemeSpecificPart)
    val locationScheme = locationURI.getScheme
    val actualFormat = format match {
      case "avro" => "com.databricks.spark.avro"
      case format: String => format
    }
    locationScheme match {
      case "hdfs" if actualFormat == "text" =>
        Ok("No Scheme Available").as("text/plain")
      case "hdfs" =>
        val location = locationURI.getSchemeSpecificPart
        val df = sparkSession.read.format(actualFormat).load(location)
        val schema = SchemaConverters.convertStructToAvro(df.schema, SchemaBuilder.record("topLevelRecord"), "")
        Ok(schema.toString(true)).as(JSON)
      case _ =>
        Ok(Json.toJson(""))
    }
  }

}
