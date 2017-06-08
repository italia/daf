package it.gov.daf.ingestionmanager

import java.io.File

import com.databricks.spark.avro.SchemaConverters
import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.datastructures.Schema
import org.apache.spark.sql.functions.{col, expr}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SaveMode}
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
  val sc = new SparkContext(conf)
  val sqlContext = new org.apache.spark.sql.SQLContext(sc)


  private def readCSV(path: String, customSchema: StructType, isHeaderDefined: Boolean = true, sep: String = ",") = Try {
    sqlContext.read
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
//      case DatasetType.RAW => //not yet implemented
//        df
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
    new Parser().parse(string)
  }

//  def write(schema: String, file: File, sep: String = ",", isHeaderDefined: Boolean = true): Try[Boolean] =  Try{
//
//    val jObjectSchema: JValue = parse(schema).asInstanceOf[JObject]
//    val dbAvroSchema = (jObjectSchema \ "dataschema").toOption.map(_.values.toString)
//    val physicalUrl = (jObjectSchema \ "operational" \ "physical_uri").toOption.map(_.values.toString)
//    val isStd = (jObjectSchema \ "operational" \ "is_std").toOption.map(_.asInstanceOf[Boolean])
//    val implementStdSchema = (jObjectSchema \ "operational" \ "std_schema").toOption match {
//      case Some(_) => true
//      case None => false
//    }
//    val rightsHolder = (jObjectSchema \ "dcatapit" \ "dct_rightsHolder" \ "value").toOption.map(_.values.toString)
//
//
//    implicit val formats = DefaultFormats
//
//    val avroSchema = readAvroSchema(org.json4s.jackson.Serialization.write(dbAvroSchema))
//
//    //val inputPath = schema.operational.get.inputSrc.url
//    //val outputPath = datastructures.convertToUriDatabase(schema).get.getUrl() // schema.convertToUriDataset().get.getUrl()
//
//    val customSchema: StructType = SchemaConverters.toSqlType(avroSchema).dataType.asInstanceOf[StructType]
//
//    val res = for {
//      df <- readCSV(file.getAbsolutePath, customSchema, isHeaderDefined, sep)
//      path <- physicalUrl.map(Success(_)).getOrElse(Failure(new Throwable("No physicalUrl into catalog entry")))
//      rh <- rightsHolder.map(Success(_)).getOrElse(Failure(new Throwable("No physicalUrl into catalog entry")))
//      schema = Schema(isStd.getOrElse(false), implementStdSchema, path, rh)
//      out <- writeDF(df, schema, path)
//    } yield out
//
//
//    res match {
//      case Success(_) =>
//       Logger.info(s"Dataframe correctly stored in ${physicalUrl.get}")
//        true
//      case Failure(ex) =>
//        Logger.error(s"Dataset reading failed due a Conversion Exception ${ex.getMessage} \n${ex.getStackTrace.mkString("\n\t")}")
//        false
//    }
//  }
//
//  def connect(WS: WSClient) (baseUrl: String) = {
//    WS.url(s"$baseUrl").get().map({ resp =>
//      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[String]
//      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
//    })
//  }

  def write(schema: MetaCatalog, file: File, sep: String = ",", isHeaderDefined: Boolean = true): Try[Boolean] =  Try{

    val dbAvroSchema = schema.dataschema
    val isStd_implementSchema = schema.operational.map(x => (x.is_std.getOrElse(false), x.std_schema.isDefined))
    val physicalUrl = schema.operational.map(_.physical_uri.getOrElse(""))
    val rightsHolder = for{
      dct <- schema.dcatapit
      rh <- dct.dct_rightsHolder
      r <- rh.id
    } yield r

//    val jObjectSchema: JValue = parse(schema).asInstanceOf[JObject]
//    val dbAvroSchema = (jObjectSchema \ "dataschema").toOption.map(_.values.toString)
//    val physicalUrl = (jObjectSchema \ "operational" \ "physical_uri").toOption.map(_.values.toString)
//    val isStd = (jObjectSchema \ "operational" \ "is_std").toOption.map(_.asInstanceOf[Boolean])
//    val implementStdSchema = (jObjectSchema \ "operational" \ "std_schema").toOption match {
//      case Some(_) => true
//      case None => false
//    }
//    val rightsHolder = (jObjectSchema \ "dcatapit" \ "dct_rightsHolder" \ "value").toOption.map(_.values.toString)


    implicit val formats = DefaultFormats

    val avroSchema = readAvroSchema(org.json4s.jackson.Serialization.write(dbAvroSchema))

    //val inputPath = schema.operational.get.inputSrc.url
    //val outputPath = datastructures.convertToUriDatabase(schema).get.getUrl() // schema.convertToUriDataset().get.getUrl()

    val customSchema: StructType = SchemaConverters.toSqlType(avroSchema).dataType.asInstanceOf[StructType]

    val res = for {
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
