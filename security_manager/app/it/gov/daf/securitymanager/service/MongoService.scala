package it.gov.daf.securitymanager.service

import java.util.Calendar

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports.DBObject
import com.mongodb.{BasicDBObject, ServerAddress}
import com.mongodb.casbah.{Imports, MongoClient, MongoCredential}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import security_manager.yaml.IpaUser

object MongoService {


  private val server = new ServerAddress(ConfigReader.getDbHost, ConfigReader.getDbPort)
  private val userName = ConfigReader.userName
  private val dbName = ConfigReader.database
  private val password = ConfigReader.password
  private val credentials = MongoCredential.createCredential(userName, dbName, password.toCharArray)
  private val USER_COLLECTION_NAME = "PreRegistratedUsers"
  private val RESETPWD_COLLECTION_NAME = "ResetPwdRequests"
  private val CATALOG_COLLECTION_NAME = "catalog_test"
  private val PRE_REGISTRATION_TTL = 60*60*2
  private val RESET_PWD_TTL = 60*60*2


  def writeUserData(user:IpaUser, token:String): Either[String,String] = {

    val document = new BasicDBObject("token", token).append("uid", user.uid).append("userpassword",user.userpassword).append("givenname",user.givenname).append("sn",user.sn).append("mail",user.mail).append("createdOn", Calendar.getInstance().getTime() )
    writeData( document, USER_COLLECTION_NAME, PRE_REGISTRATION_TTL )

  }

  def writeResetPwdData(user:IpaUser, token:String): Either[String,String] = {

    val document = new BasicDBObject("token", token).append("uid", user.uid).append("mail",user.mail).append("createdOn", Calendar.getInstance().getTime() )
    writeData( document, RESETPWD_COLLECTION_NAME, RESET_PWD_TTL )

  }

  private def writeData( document:BasicDBObject, collectionName:String, ttl:Int)={

    val mongoClient = MongoClient( server, List(credentials) )
    val db = mongoClient(dbName)
    val coll = db(collectionName)


    if(coll.isEmpty) {
      try
        coll.underlying.createIndex(new BasicDBObject("createdOn", 1), new BasicDBObject("expireAfterSeconds", ttl))
      catch{ case e:Exception => Logger.logger.warn("Index already created") }
    }

    val inserted = coll.insert(document)
    mongoClient.close()

    Logger.logger.debug( "mongo write result: "+inserted )

    if(! inserted.isUpdateOfExisting )
      Right("ok")
    else
      Left("ko")

  }

  def addACL( datasetName:String, groupName:String, groupType:String, permission:String ): Either[String,String] = {

    //$push:{"operational.acl":{"groupName":"comune_torino","type":"organization","permissions":"rw"}}

    val query = MongoDBObject("dcatapit.name" -> datasetName)
    val aclPermission = MongoDBObject("groupName"->groupName,"groupType"->groupType,"permission"->permission)
    val update = Imports.$push("operational.acl" -> aclPermission )
    updateData( query, update, CATALOG_COLLECTION_NAME )

  }

  def removeACL( datasetName:String, groupName:String, permissions:String ): Either[String,String] = {

    val query = MongoDBObject("dcatapit.name" -> datasetName)
    val update = Imports.$pull("operational.acl" -> ("groupName"->groupName) )
    updateData( query, update, CATALOG_COLLECTION_NAME )

  }

  def getACL(datasetName:String): Either[String,JsValue] = {

    val result = findData( "dcatapit.name", datasetName, CATALOG_COLLECTION_NAME )
    result match{
      case Right(json) => ((json \ "operational") \ "acl").toOption match{
        case Some(x) => Right(x)
        case None =>  Logger.logger.warn( "No Acl found: "+result )
                      Left("No Acl found")
      }
      case _ => result
    }

  }


  private def updateData( query:DBObject, update:DBObject, collectionName:String )={

    val mongoClient = MongoClient( server, List(credentials) )
    val db = mongoClient(dbName)
    val coll = db(collectionName)

    val updated = coll.update(query, update)
    mongoClient.close()

    Logger.logger.debug( "mongo update result: "+updated )

    if(updated.isUpdateOfExisting)
      Right("ok")
    else
      Left("ko")

  }

  def findUserByToken(token:String): Either[String,JsValue] = {
    findData(USER_COLLECTION_NAME,"token",token)
  }

  def findAndRemoveUserByToken(token:String): Either[String,JsValue] = {
    findAndRemoveData(USER_COLLECTION_NAME,"token",token)
  }

  def findAndRemoveResetPwdByToken(token:String): Either[String,JsValue] = {
    findAndRemoveData(RESETPWD_COLLECTION_NAME,"token",token)
  }

  def findUserByUid(uid:String): Either[String,JsValue] = {
    findData(USER_COLLECTION_NAME,"uid",uid)
  }

  def findResetPwdByMail(mail:String): Either[String,JsValue] = {
    findData(RESETPWD_COLLECTION_NAME,"mail",mail)
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
      case None => Left(s"Data in $collectionName not found")
    }

  }

  private def findAndRemoveData(collectionName:String, filterAttName:String, filterValue:String): Either[String,JsValue] = {

    val mongoClient = MongoClient(server,List(credentials))
    val db = mongoClient(dbName)

    val coll = db(collectionName)

    val query = MongoDBObject(filterAttName -> filterValue)
    val result = coll.findAndRemove(query)
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
