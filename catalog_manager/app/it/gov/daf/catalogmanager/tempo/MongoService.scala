package it.gov.daf.catalogmanager.tempo

import java.util.Calendar

import catalog_manager.yaml.IpaUser
import com.mongodb.casbah.{MongoClient, MongoCollection}
import com.mongodb.{BasicDBObject, MongoCredential, ServerAddress}
import com.mongodb.casbah.commons.MongoDBObject
import it.gov.daf.catalogmanager.utilities.ConfigReader
import it.gov.daf.catalogmanager.utilities.Prova3.{credentials, server}
import play.api.libs.json.{JsString, JsValue, Json}

object MongoService {


  private val server = new ServerAddress(ConfigReader.getDbHost, ConfigReader.getDbPort)
  private val userName = ConfigReader.userName
  private val dbName = ConfigReader.database
  private val password = ConfigReader.password
  private val USER_COLLECTION_NAME = "PreRegistratedUsers"
  private val PRE_REGISTRATION_TTL = 60*60*2


  def writeUserData(user:IpaUser, token:String): Boolean = {

    val mongoClient = MongoClient( server, List(credentials) )
    val db = mongoClient(dbName)
    val coll = db(USER_COLLECTION_NAME)


    if(coll.isEmpty) {
      try
        coll.underlying.createIndex(new BasicDBObject("createdOn", 1), new BasicDBObject("expireAfterSeconds", PRE_REGISTRATION_TTL))
      catch{ case e:Exception => println("Indice già creato") }
    }

    val document = new BasicDBObject("token", token).append("uid", user.uid).append("userpassword",user.userpassword).append("givenname",user.givenname).append("sn",user.sn).append("mail",user.mail).append("createdOn", Calendar.getInstance().getTime() )
    val inserted = coll.insert(document)
    mongoClient.close()
    inserted.getN > 0

  }

  def findUserByToken(token:String): Either[String,JsValue] = {
    findData(USER_COLLECTION_NAME,"token",token)
  }

  def findUserByUid(uid:String): Either[String,JsValue] = {
    findData(USER_COLLECTION_NAME,"uid",uid)
  }



  private def findData(collectionName:String, filterAttName:String, filterValue:String): Either[String,JsValue] = {

    val mongoClient = MongoClient(server,List(credentials))
    val db = mongoClient(dbName)

    val coll = db(collectionName)

    val query = MongoDBObject(filterAttName -> filterValue)
    val result = coll.findOne(query)
    mongoClient.close

    result match {
      case Some(x) => {
        val jsonString = com.mongodb.util.JSON.serialize(x)
        Right(Json.parse(jsonString))
      }
      case None => Left("Not found")
    }

  }


  /*
  private def readMongoById(collectionName: String, id: String): JsValue = {

    val mongoClient = MongoClient(server,List(credentials))
    val db = mongoClient(dbName)
    //val collection = db.getCollection(collectionName)
    val coll = db(collectionName)
    //val result2 = collection.findOne(equal(filterAttName, filterValue))

    val query = MongoDBObject("_id" -> new ObjectId(id))
    val result = coll.findOne(query)
    mongoClient.close

    val out: JsValue = result match {
      case Some(x) => {
        val jsonString = com.mongodb.util.JSON.serialize(x)
        Json.parse(jsonString)
      }
      case None => JsString("Not found")
    }
    out
  }*/



}
