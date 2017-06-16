package it.gov.daf.ingestionmanager

import java.io.File

import com.databricks.spark.avro.SchemaConverters
import it.gov.daf.catalogmanager.{Avro, MetaCatalog}
import it.gov.daf.datastructures.Schema
import org.apache.spark.sql.functions.{col, expr}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import org.apache.avro.Schema.Parser
import org.json4s.JsonAST.JValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import play.Logger



/**
  * Created by fabiana on 23/05/17.
  */
class IngestionManager {

  val conf = new SparkConf().setAppName("Injestion").setMaster("local[2]")

  private val sparkSession = SparkSession.builder().master("local").config(conf).getOrCreate()


  private def readCSV(path: String, customSchema: StructType, isHeaderDefined: Boolean = true, sep: String = ",") = Try {
    sparkSession.read
      .format("com.databricks.spark.csv")
      .option("header", isHeaderDefined) // Use first line of all files as header
      .option("delimiter", sep)
      .schema(customSchema)
      //.option("inferSchema", "true") // Automatically infer data types
      .load(path)
  }

  private def enrichDataFrame(df: DataFrame, schema: Schema) : (DataFrame, List[String]) = {
    val timestamp: Long = System.currentTimeMillis / 1000

    schema.isStd match {
      case true =>
        val newdf = df.withColumn("owner", expr("'" + schema.rightHolder + "'"))
          .withColumn("ts", expr("'" + timestamp + "'"))
        val partitionList = List("owner", "ts")
        (newdf, partitionList)
      case _ =>
        val newdf = df.withColumn("ts", expr("'" + timestamp + "'"))
        val partitionList = List("ts")
        (newdf, partitionList)

    }
  }

  private def writeDF(df:DataFrame, schema: Schema, filePath: String) = Try{

    val (enrichedDF, partitionList) = enrichDataFrame(df, schema)

    val repartitionList = partitionList.map(x => col(x))
    enrichedDF
      .repartition(repartitionList:_*)  //questa riga ha un costo rilevante
      .write
      .partitionBy(partitionList:_*)
      .mode(SaveMode.Append)
      .parquet(filePath + ".parquet")
  }

  def readAvroSchema(string: String): org.apache.avro.Schema = {
    println(string)
    new Parser().parse(string)
  }


  def write(schema: MetaCatalog, file: File, sep: String = ",", isHeaderDefined: Boolean = true): Try[Boolean] =  Try{

    val dbAvroSchema = schema.dataschema.flatMap(_.avro)
    val isStd_implementSchema = schema.operational.map(x => (x.is_std.getOrElse(false), x.std_schema.isDefined))
    val physicalUrl = schema.operational.map(_.physical_uri.getOrElse(""))
    val rightsHolder = for{
      dct <- schema.dcatapit
      rh <- dct.dct_rightsHolder
      r <- rh.value
    } yield r



    implicit val formats = DefaultFormats

    val res = for {
      tryAvroSchema <- dbAvroSchema.map(Success(_)).getOrElse(Failure(new Throwable("No Avro schema")))
      avroSchema <- Try(readAvroSchema(org.json4s.jackson.Serialization.write(dbAvroSchema)))
      customSchema = SchemaConverters.toSqlType(avroSchema).dataType.asInstanceOf[StructType]
      df <- readCSV(file.getAbsolutePath, customSchema, isHeaderDefined, sep)
      path <- physicalUrl.map(Success(_)).getOrElse(Failure(new Throwable("No physicalUrl into catalog entry")))
      rh <- rightsHolder.map(Success(_)).getOrElse(Failure(new Throwable("No rightHolder into catalog entry")))
      (isStd, implementStdSchema) <- isStd_implementSchema.map(Success(_)).getOrElse(Failure(new Throwable("No schema information into catalog entry")))
      schema = Schema(isStd, implementStdSchema, path, rh)
      out <- writeDF(df, schema, path)
    } yield out


    res match {
      case Success(_) =>
        Logger.info(s"Dataframe correctly stored in ${physicalUrl.get}")
        true
      case Failure(ex) =>
        Logger.error(s"Dataset reading failed due a Conversion Exception ${ex.getMessage} \n${ex.getStackTrace.mkString("\n\t")}")
        false
    }
  }
}
