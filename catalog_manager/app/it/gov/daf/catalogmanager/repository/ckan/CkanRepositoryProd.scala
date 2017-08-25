package it.gov.daf.catalogmanager.repository.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import catalog_manager.yaml.{Credentials, Dataset, MetadataCat, Organization, ResourceSize, User}
import com.mongodb.{DBObject, MongoCredential, ServerAddress}
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import play.api.libs.ws.ahc.AhcWSClient
import it.gov.daf.catalogmanager.utilities.{ConfigReader, SecurePasswordHashing, WebServiceUtil}
import play.api.libs.json._

import scala.concurrent.Future

/**
  * Created by ale on 11/05/17.
  */

class CkanRepositoryProd extends CkanRepository{

  import catalog_manager.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private val LOCALURL = ConfigReader.localUrl
  private val CKAN_ERROR = "CKAN service is not working correctly"

  private val server = new ServerAddress(ConfigReader.getDbHost, ConfigReader.getDbPort)
  private val userName = ConfigReader.userName
  private val dbName = ConfigReader.database
  private val password = ConfigReader.password
  private val credentials = MongoCredential.createCredential(userName, dbName, password.toCharArray)

  private val USER_ID_HEADER:String = ConfigReader.userIdHeader

  private def writeMongo(json: JsValue, collectionName: String): Boolean = {

    println("write mongo: "+json.toString())
    //val mongoClient = MongoClient(mongoHost, mongoPort)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(dbName)
    val coll = db(collectionName)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    val inserted = coll.insert(obj)
    mongoClient.close()
    inserted.getN > 0

  }

  private def readMongo(collectionName: String, filterAttName: String, filterValue: String): JsValue = {

    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(dbName)
    //val collection = db.getCollection(collectionName)
    val coll = db(collectionName)
    //val result2 = collection.findOne(equal(filterAttName, filterValue))

    val query = MongoDBObject(filterAttName -> filterValue)
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

  }

  def getMongoUser(name:String, callingUserid :MetadataCat): JsResult[User] = {

    var jsUser = readMongo("users","name", name )
    jsUser = jsUser.as[JsObject] ++ Json.obj("password" -> "")
    val userValidate = jsUser.validate[User]
    userValidate

  }

  def verifyCredentials(credentials: Credentials):Boolean = {

    val result = readMongo("users", "name", credentials.username.get.toString)
    val hpasswd = (result \ "password").get.as[String]

    //println("pwd: "+credentials.password.get.toString)
    //println("hpasswd: "+hpasswd)

    return SecurePasswordHashing.validatePassword(credentials.password.get.toString, hpasswd )
  }

  def updateOrganization(orgId: String, jsonOrg: JsValue, callingUserid :MetadataCat): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/updateOrganization/" + orgId
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).put(jsonOrg).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def createUser(jsonUser: JsValue, callingUserid :MetadataCat): Future[String] = {

    val password = (jsonUser \ "password").get.as[String]
    println("PWD -->"+password+"<")
    val hashedPassword = SecurePasswordHashing.hashPassword( password )
    val updatedJsonUser = jsonUser.as[JsObject] + ("password" -> JsString(hashedPassword) )

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createUser"
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).post(jsonUser).map({ response =>
      (response.json \ "success").toOption match {
        case Some(x) => if(x.toString()=="true")writeMongo(updatedJsonUser,"users"); x.toString()
        case _ => JsString(CKAN_ERROR).toString()
      }
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getUserOrganizations(userName :String, callingUserid :MetadataCat) : Future[JsResult[Seq[Organization]]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/userOrganizations/" + userName
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>



      val orgsListJson: JsLookupResult = response.json \ "result"

      if(orgsListJson.toOption.isEmpty)
        JsError( WebServiceUtil.getMessageFromCkanError(response.json) )
      else
        orgsListJson.get.validate[Seq[Organization]]

      /*
      val orgsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))

      val orgsListValidate = orgsListJson.validate[Seq[Organization]]

      orgsListValidate*/

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }

  def createDataset(jsonDataset: JsValue, callingUserid :MetadataCat): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createDataset"
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).post(jsonDataset).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def createOrganization(jsonDataset: JsValue, callingUserid :MetadataCat): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createOrganization"
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).post(jsonDataset).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }

  def dataset(datasetId: String, callingUserid :MetadataCat): JsValue = JsString("TODO")


  def getOrganization(orgId :String, callingUserid :MetadataCat) : Future[JsResult[Organization]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/organization/" + orgId
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>

      val orgJson: JsLookupResult = response.json \ "result"

      if(orgJson.toOption.isEmpty)
        JsError( WebServiceUtil.getMessageFromCkanError(response.json) )
      else
        orgJson.get.validate[Organization]

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getOrganizations(callingUserid :MetadataCat) : Future[JsValue] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/organizations"
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>
      val orgsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))
      orgsListJson
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }


  def getDatasets(callingUserid :MetadataCat) : Future[JsValue] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/datasets"
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>
      val dsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))
      dsListJson
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }

  def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize), callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]]={

    val wsClient = AhcWSClient()

    val params = Map(("q",input._1),("sort",input._2),("rows",input._3))

    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    val url =  LOCALURL + "/ckan/searchDataset"+queryString

    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>

      val datasetJson: JsLookupResult = ( (response.json \ "result") \ "results")

      if(datasetJson.toOption.isEmpty)
        JsError( WebServiceUtil.getMessageFromCkanError(response.json) )
      else
        datasetJson.get.validate[Seq[Dataset]]

      /*
      val datasetJson: JsValue =( (response.json \ "result") \ "results")
        .getOrElse(Json.obj("error" -> "No datasets"))

      val datasetsValidate = datasetJson.validate[Seq[Dataset]]
      println(datasetsValidate)
      datasetsValidate*/
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getDatasetsWithRes( input: (ResourceSize, ResourceSize), callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]] = {

    val wsClient = AhcWSClient()

    val params = Map( ("limit",input._1),("offset",input._2) )

    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    val url =  LOCALURL + "/ckan/datasetsWithResources"+queryString

    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>


      val datasetJson: JsLookupResult = response.json \ "result"

      if(datasetJson.toOption.isEmpty)
        JsError( WebServiceUtil.getMessageFromCkanError(response.json) )
      else
        datasetJson.get.validate[Seq[Dataset]]

      /*
      val datasetJson: JsValue =(response.json \ "result")
        .getOrElse(Json.obj("error" -> "No datasets"))

      val datasetsValidate = datasetJson.validate[Seq[Dataset]]
      //println(datasetsValidate)
      datasetsValidate*/
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


  def testDataset(datasetId :String, callingUserid :MetadataCat) : Future[JsResult[Dataset]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/dataset/" + datasetId

    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>
      val datasetJson: JsValue = (response.json \ "result")
        .getOrElse(Json.obj("error" -> "No dataset"))
      val datasetValidate = datasetJson.validate[Dataset]
      datasetValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


}

