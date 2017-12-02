package it.gov.daf.catalogmanager.repository.catalog

import catalog_manager.yaml.{Dataset, DatasetCatalogFlatSchema, MetaCatalog, MetadataCat, ResponseWrites, Success}
import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import org.bson.types.ObjectId
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import com.mongodb.casbah.Imports._
import it.gov.daf.catalogmanager.utilities.{CatalogManager, ConfigReader, SftpUtils}
import it.gov.daf.catalogmanager.service.CkanRegistry
import net.schmizz.sshj.SSHClient

import scala.concurrent.Future
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

  val server = new ServerAddress(mongoHost, 27017)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

  import catalog_manager.yaml.BodyReads._

  def listCatalogs(page :Option[Int], limit :Option[Int]) :Seq[MetaCatalog] = {

    val mongoClient = MongoClient(server, List(credentials))
    //val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient(source)
    val coll = db("catalog_test")
    val results = coll.find()
        .skip(page.getOrElse(0))
        .limit(limit.getOrElse(200))
        .toList
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


  def catalog(logicalUri :String): Option[MetaCatalog] = {
    //val objectId : ObjectId = new ObjectId(catalogId)
    val query = MongoDBObject("operational.logical_uri" -> logicalUri)
    // val mongoClient = MongoClient(mongoHost, mongoPort)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("catalog_test")
    val result = coll.findOne(query)
    mongoClient.close
    val metaCatalog: Option[MetaCatalog] = result match {
      case Some(x) => {
        val jsonString = com.mongodb.util.JSON.serialize(x)
        val json = Json.parse(jsonString) //.as[List[JsObject]]
        val metaCatalogJs = json.validate[MetaCatalog]
        val metaCatalog = metaCatalogJs match {
          case s: JsSuccess[MetaCatalog] => Some(s.get)
          case _: JsError => None
        }
        metaCatalog
      }
      case _ => None
    }
    metaCatalog
  }

  def createCatalog(metaCatalog: MetaCatalog, callingUserid :MetadataCat) :Success = {

    import catalog_manager.yaml.ResponseWrites.MetaCatalogWrites

    //val mongoClient = MongoClient(mongoHost, mongoPort)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("catalog_test")

    val dcatapit: Dataset = metaCatalog.dcatapit
    val datasetJs : JsValue = ResponseWrites.DatasetWrites.writes(dcatapit)

    val result: Future[String] = CkanRegistry.ckanRepository.createDataset(datasetJs,callingUserid)

    val msg = if(metaCatalog.operational.std_schema.isDefined) {
      val stdUri = metaCatalog.operational.std_schema.get.std_uri
      //TODO Review logic
      val stdCatalot: MetaCatalog = catalog(stdUri).get
      val res: Option[MetaCatalog] = CatalogManager.writeOrdinaryWithStandard(metaCatalog, stdCatalot)

      val message = res match {
        case Some(meta) =>
          val json: JsValue = MetaCatalogWrites.writes(meta)
          val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
          val inserted = coll.insert(obj)
          mongoClient.close()
          val msg = meta.operational.logical_uri
          msg
        case _ =>
          println("Error");
          val msg = "Error"
          msg
      }
      message
    } else {
      val random = scala.util.Random
      val id = random.nextInt(1000).toString
      val res: Option[MetaCatalog]= (CatalogManager.writeOrdinary(metaCatalog))
      val message = res match {
        case Some(meta) =>
          val json: JsValue = MetaCatalogWrites.writes(meta)
          val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
          val inserted = coll.insert(obj)
          val msg = meta.operational.logical_uri
          msg
        case _ =>
          println("Error");
          val msg = "Error"
          msg
      }
      message
    }

   // SftpUtils.createDirs(metaCatalog)

    /*
    val ssh = new SSHClient
    ssh.loadKnownHosts()
    ssh.connect("edge1")
    ssh.authPassword("alessandro", "silviale7881")
    val sftp = ssh.newSFTPClient()
    //val dirPath = "/home/" + groupOwn + "/" + theme + "/" + subtheme "/" + filename
    sftp.mkdirs("/home/alessandro/" + metaCatalog.operational.theme + "/" + metaCatalog.operational.subtheme + "/" + metaCatalog.dcatapit.name)
*/
    Success(msg, Some(msg))
  }


  // Not used implement query on db
  def standardUris(): List[String] = List("raf", "org", "cert")

}
