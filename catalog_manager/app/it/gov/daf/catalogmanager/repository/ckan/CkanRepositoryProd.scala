package it.gov.daf.catalogmanager.repository.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import catalog_manager.yaml.{AutocompRes, Credentials, Relationship, Dataset, Error, MetadataCat, Organization, ResourceSize, Success, User}
import com.mongodb.{DBObject, MongoCredential, ServerAddress}
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import play.api.libs.ws.ahc.AhcWSClient
import it.gov.daf.catalogmanager.utilities.{ConfigReader, SecurePasswordHashing}
import it.gov.daf.common.utils.WebServiceUtil
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient
import java.time.{Instant, ZoneOffset}

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

  private val CKAN_GEO_URL = ConfigReader.getCkanGeoUrl

  private val server = new ServerAddress(ConfigReader.getDbHost, ConfigReader.getDbPort)
  private val userName = ConfigReader.userName
  private val dbName = ConfigReader.database
  private val password = ConfigReader.password
  private val credentials = MongoCredential.createCredential(userName, dbName, password.toCharArray)

  private val USER_ID_HEADER:String = ConfigReader.userIdHeader

  private def writeMongo(json: JsValue, collectionName: String): Boolean = {

    Logger.debug("write mongo: "+json.toString())
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

    if( !( jsUser \ "password").toOption.isEmpty )
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

  private def evaluateSuccessResult(json:JsValue)={
    val resultJson = (json \ "success").toOption

    if( !resultJson.isEmpty && resultJson.get.toString() == "true" )
      "true"
    else
      WebServiceUtil.getMessageFromCkanError(json)
  }

  private def evaluateSuccessResult(json:JsValue, onSuccessDo: => Boolean) = {
    val resultJson = (json \ "success").toOption

    if( !resultJson.isEmpty && resultJson.get.toString() == "true" ) {
      onSuccessDo
      "true"
    }else
      WebServiceUtil.getMessageFromCkanError(json)
  }

  def updateOrganization(orgId: String, jsonOrg: JsValue, callingUserid :MetadataCat): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/updateOrganization/" + orgId
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).put(jsonOrg).map({ response =>

      evaluateSuccessResult(response.json)

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def patchOrganization(orgId: String, jsonOrg: JsValue, callingUserid :MetadataCat): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/patchOrganization/" + orgId
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).put(jsonOrg).map({ response =>

      evaluateSuccessResult(response.json)

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def createUser(jsonUser: JsValue, callingUserid :MetadataCat): Future[String] = {

    val password = (jsonUser \ "password").get.as[String]

    val hashedPassword = SecurePasswordHashing.hashPassword( password )
    val updatedJsonUser = jsonUser.as[JsObject] + ("password" -> JsString(hashedPassword) )

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createUser"
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).post(jsonUser).map({ response =>

      evaluateSuccessResult( response.json, writeMongo(updatedJsonUser,"users") )

      /*
      (response.json \ "success").toOption match {
        case Some(x) => if(x.toString()=="true")writeMongo(updatedJsonUser,"users"); x.toString()
        case _ => JsString(CKAN_ERROR).toString()
      }*/

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

      evaluateSuccessResult(response.json)

    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def createDatasetCkanGeo(catalog: Dataset, user: String, token: String, wsClient: WSClient): Future[Either[Error, Success]] = {
    import java.time.LocalDateTime

    def formatTimestap(timestamp: String) = {
      val arrayTimestamp = timestamp.split('.')
      val sec = arrayTimestamp(1)
      val secOut = sec.length match {
        case x if x > 6 => sec.substring(0, 6)
        case x if x < 6 => sec + "0".*(6-x)
        case _ => sec
      }
      List(arrayTimestamp(0), secOut).mkString(".")
    }

    val url = LOCALURL + "/ckan/createDataset"
    val timestamp = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toString
    val jsonString = catalog_manager.yaml.ResponseWrites.DatasetWrites.writes(
      catalog.copy(
        relationships_as_object = Some(Seq(Relationship(None, None, Some("has_derivation"), Some("opendataDaf")))),
        resources = Some(catalog.resources.get.map(f => f.copy(created = Some(formatTimestap(timestamp)))))
    )).toString().replace("`", "")
    val json = Json.parse(jsonString)
    Logger.logger.debug(s"json: $json")

    wsClient.url(url).withHeaders("authorization" -> token).post(json).map{ response =>
      if(response.status == 200) Right(Success(s"${catalog.name} inserted", None))
      else Left(Error(s"error: ${response.statusText}", None, None))
    }
  }

  def deleteDatasetCkanGeo(catalog: Dataset, user: String, token: String, wsClient: WSClient): Future[Either[Error, Success]] = {
    val url = LOCALURL + "/ckan/purgeDatasetCkanGeo/" + catalog.name
    wsClient.url(url).withHeaders("authorization" -> token).delete().map{ response =>
      if(response.status == 200) Right(Success(s"${catalog.name} deleted by $user", None))
      else Left(Error(s"error: ${response.statusText}", None, None))
    }
  }

  def createOrganization(jsonDataset: JsValue, callingUserid :MetadataCat): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createOrganization"
    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).post(jsonDataset).map({ response =>

      evaluateSuccessResult(response.json)

      //(response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
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

  def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize, ResourceSize), callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]]={

    val wsClient = AhcWSClient()

    val params = Map(("q",input._1),("sort",input._2),("rows",input._3), ("start",(input._4)))

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

  def autocompleteDatasets( input: (MetadataCat, ResourceSize), callingUserid :MetadataCat) : Future[JsResult[Seq[AutocompRes]]] = {
    val wsClient = AhcWSClient()

    val params = Map(("q",input._1),("limit",input._2))

    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    val url =  LOCALURL + "/ckan/autocompleteDataset"+queryString

    wsClient.url(url).withHeaders(USER_ID_HEADER -> callingUserid.get).get().map ({ response =>

      val datasetJson: JsLookupResult = (response.json \ "result")

      if(datasetJson.toOption.isEmpty)
        JsError( WebServiceUtil.getMessageFromCkanError(response.json) )
      else
        datasetJson.get.validate[Seq[AutocompRes]]

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

      val datasetJson: JsLookupResult = response.json \ "result"

      if(datasetJson.toOption.isEmpty)
        JsError( WebServiceUtil.getMessageFromCkanError(response.json) )
      else
        datasetJson.get.validate[Dataset]


    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


}

