package it.gov.daf.catalogmanager.repository.catalog

import java.io.{FileInputStream, FileWriter}

import catalog_manager.yaml._
import play.Environment
import play.api.libs.json._
import play.api.libs.ws.WSClient


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

  val dcatJson: JsResult[Dataset] = dcatSchema.validate[Dataset]
  private val dcat = dcatJson match {
    case s: JsSuccess[Dataset] => Option(s.get)
    case e: JsError => None
  }

  def listCatalogs(page :Option[Int], limit :Option[Int]) :Seq[MetaCatalog] = {
   // Seq(MetaCatalog(datasetCatalog,operational,conversion,dcat))
    (datasetCatalog, operational, dcat) match {
      case (Some(dsCat), Some(op), Some(dcatapit)) => Seq(MetaCatalog(dsCat,op,dcatapit))
      case _ => Seq()
    }
  }

  def catalog(catalogId :String): Option[MetaCatalog] = {
  // MetaCatalog(datasetCatalog,operational,conversion,dcat)
    for {
      dc <- datasetCatalog
      op <- operational
      dcatapit <- dcat
    } yield MetaCatalog(dc, op, dcatapit)

  }

  def catalogByName(catalogByName :String): Option[MetaCatalog] = {
    // MetaCatalog(datasetCatalog,operational,conversion,dcat)
    for {
      dc <- datasetCatalog
      op <- operational
      dcatapit <- dcat
    } yield MetaCatalog(dc, op, dcatapit)

  }

  def createCatalog(metaCatalog: MetaCatalog, callingUserid :MetadataCat, ws :WSClient) :Success = {
    Success("Created",None)
  }

  def standardUris(): List[String] = List("raf", "org", "cert")

}
