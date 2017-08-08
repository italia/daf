package it.gov.daf.catalogmanager.repository.catalog

import catalog_manager.yaml.{Dataset, MetaCatalog, ResponseWrites, Success}
import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import org.bson.types.ObjectId
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import com.mongodb.casbah.Imports._
import it.gov.daf.catalogmanager.utilities.{CatalogManager, ConfigReader}
import it.gov.daf.catalogmanager.service.CkanRegistry

import scala.util.Try


/**
  * Created by ale on 18/05/17.
  */
class CatalogRepositoryMongo extends  CatalogRepository{

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort
  private val database = ConfigReader.getDbHost

  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password

  val server = new ServerAddress("localhost", 27017)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)



  import catalog_manager.yaml.BodyReads._

  def listCatalogs() :Seq[MetaCatalog] = {

    val mongoClient = MongoClient(server, List(credentials))
    //val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient(source)
    val coll = db("catalog_test")
    val results = coll.find().toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString) //.as[List[JsObject]]
    val metaCatalogJs = json.validate[Seq[MetaCatalog]]
    val metaCatalog = metaCatalogJs match {
      case s: JsSuccess[Seq[MetaCatalog]] => s.get
      case e: JsError => Seq()
    }
    metaCatalog
   // Seq(MetaCatalog(None,None,None))
  }


  def getCatalogs(logicalUri :String) :MetaCatalog = {
    //val objectId : ObjectId = new ObjectId(catalogId)
    val query = MongoDBObject("operational.logical_uri" -> logicalUri)
   // val mongoClient = MongoClient(mongoHost, mongoPort)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient("catalog_manager")
    val coll = db("catalog_test")
    val result = coll.findOne(query)
    mongoClient.close
    val metaCatalog: MetaCatalog = result match {
      case Some(x) => {
        val jsonString = com.mongodb.util.JSON.serialize(x)
        val json = Json.parse(jsonString) //.as[List[JsObject]]
        val metaCatalogJs = json.validate[MetaCatalog]
        val metaCatalog = metaCatalogJs match {
          case s: JsSuccess[MetaCatalog] => s.get
          case _: JsError => MetaCatalog(None,None,None)
        }
        metaCatalog
      }
      case None => MetaCatalog(None,None,None)
    }
    metaCatalog
  }

  def createCatalog(metaCatalog: MetaCatalog) :Success = {

    import catalog_manager.yaml.ResponseWrites.MetaCatalogWrites

    //val mongoClient = MongoClient(mongoHost, mongoPort)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient("catalog_manager")
    val coll = db("catalog_test")

    // After Test refactor TODO
    val dcatapit: Dataset = metaCatalog.dcatapit.get
    val datasetJs : JsValue = ResponseWrites.DatasetWrites.writes(dcatapit)
    CkanRegistry.ckanRepository.createDataset(datasetJs)

    val msg: String = metaCatalog match {
      case MetaCatalog(Some(dataSchema), Some(operational), _) =>
        if(operational.std_schema.get.std_uri.isDefined ) {
          val stdUri = operational.std_schema.get.std_uri.get
          val res: Try[(Boolean, MetaCatalog)] = Try(getCatalogs(stdUri))
            .map(CatalogManager.writeOrdinaryWithStandard(metaCatalog, _))
          res match {
            case scala.util.Success((true, meta)) =>
              val json: JsValue = MetaCatalogWrites.writes(meta)
              val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
              val inserted = coll.insert(obj)
              mongoClient.close()
              val msg = meta.operational.get.logical_uri.get
              msg
            case _ =>
              println("Error");
              val msg = "Error"
              msg
          }
        } else {
          val random = scala.util.Random
          val id = random.nextInt(1000).toString
          val res: Try[(Boolean, MetaCatalog)]= Try(CatalogManager.writeOrdinary(metaCatalog))
          val msg = res match {
            case scala.util.Success((true, meta)) =>
              val json: JsValue = MetaCatalogWrites.writes(meta)
              val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
              val inserted = coll.insert(obj)
              val msg = meta.operational.get.logical_uri.get
              msg
            case _ =>
              println("Error");
              val msg = "Error"
              msg
          }
          msg
        }
      case _ => println(""); val msg = "Error"; msg
    }

    Success(Some(msg),Some(msg))
  }

  def standardUris(): List[String] = List("raf", "org", "cert")

}
