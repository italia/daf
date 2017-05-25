package it.gov.daf.catalogmanager.repository.catalog

import java.io.{FileInputStream, FileWriter}

import catalog_manager.yaml._
import play.Environment
import play.api.libs.json._


/**
  * Created by ale on 05/05/17.
  */
class CatalogRepositoryDev extends CatalogRepository{

  private val streamDataschema =
    new FileInputStream(Environment.simple().getFile("data/data-mgt/data-dataschema.json"))
   // new FileInputStream(Environment.simple().getFile("data/data-mgt/std/std-dataschema.json"))
  private val dataschema: JsValue = try {
    Json.parse(streamDataschema)
  } finally {
    streamDataschema.close()
  }

  private val streamOperational =
    new FileInputStream(Environment.simple().getFile("data/data-mgt/data-operational.json"))
  private val operationalSchema: JsValue = try {
    Json.parse(streamOperational)
  } finally {
    streamOperational.close()
  }

  //private val streamConversion =
  //  new FileInputStream(Environment.simple().getFile("data/data-convStd.json"))
  //private val conversionSchema: JsValue = try {
  //  Json.parse(streamConversion)
  //} finally {
  //  streamConversion.close()
  //}

  private val streamDcat =
    new FileInputStream(Environment.simple().getFile("data/data-mgt/data-dcatapit.json"))
  private val dcatSchema: JsValue = try {
    Json.parse(streamDcat)
  } finally {
    streamDcat.close()
  }

  import catalog_manager.yaml.BodyReads._

  val datasetCatalogJson: JsResult[DatasetCatalog] = dataschema.validate[DatasetCatalog]
  val datasetCatalog = datasetCatalogJson match {
    case s: JsSuccess[DatasetCatalog] => println(s.get);Option(s.get)
    case e: JsError => println(e);None;
  }
  val operationalJson: JsResult[Operational] = operationalSchema.validate[Operational]
  val operational = operationalJson match {
    case s: JsSuccess[Operational] => Option(s.get)
    case e: JsError => None
  }

  //val convStdJson: JsResult[ConversionSchema] = conversionSchema.validate[ConversionSchema]
  //val conversion = convStdJson match {
  //  case s: JsSuccess[ConversionSchema] => Option(s.get)
  //  case e: JsError => None
 // }

  val dcatJson: JsResult[DcatApIt] = dcatSchema.validate[DcatApIt]
  val dcat = dcatJson match {
    case s: JsSuccess[DcatApIt] => Option(s.get)
    case e: JsError => None
  }

  def listCatalogs() :Seq[MetaCatalog] = {
   // Seq(MetaCatalog(datasetCatalog,operational,conversion,dcat))
    Seq(MetaCatalog(datasetCatalog,operational,dcat))
  }

  def getCatalogs(catalogId :String) :MetaCatalog = {
  // MetaCatalog(datasetCatalog,operational,conversion,dcat)
     MetaCatalog(datasetCatalog,operational,dcat)
  }

  def createCatalog(metaCatalog: MetaCatalog) :Successf = {
    Successf(None,None)
  }
}
