package it.gov.daf.catalogmanager.repository.catalog

import catalog_manager.yaml.{MetaCatalog, Successf}
import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import it.gov.daf.catalogmanager.utils.ConfigReader
import org.bson.types.ObjectId
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import com.mongodb.casbah.Imports._

/**
  * Created by ale on 18/05/17.
  */
class CatalogRepositoryMongo extends  CatalogRepository{



  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort

  import catalog_manager.yaml.BodyReads._

  def listCatalogs() :Seq[MetaCatalog] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("catalog_manager")
    val coll = db("catalog")
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


  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def getCatalogs(catalogId :String) :MetaCatalog = {
    //val objectId : ObjectId = new ObjectId(catalogId)
    //val query = MongoDBObject("_id" -> objectId)
    //println(query)
    println("CATALOGID")
    println(catalogId)
    val objectId : ObjectId = new ObjectId(catalogId)
    val query = MongoDBObject("_id" -> objectId)
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("catalog_manager")
    val coll = db("catalog")
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

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def createCatalog(metaCatalog: MetaCatalog) :Successf = {
    println("MongoHost : " + mongoHost)
    import catalog_manager.yaml.ResponseWrites.MetaCatalogWrites
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("catalog_manager")
    val coll = db("catalog")
    val json: JsValue = MetaCatalogWrites.writes(metaCatalog)
    println("TEST Insert")
    println(Json.stringify(json))
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    val inserted = coll.insert(obj)
    mongoClient.close()
    Successf(Option("Catalog saved"),Option("Catalog saved"))
  }
}
