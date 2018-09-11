  package it.gov.daf.catalogmanager.repository.catalog

  /**
    * Created by ale on 23/05/17.
    */

  import java.io.{File, FileInputStream, FileWriter}

  import catalog_manager.yaml._
  import it.gov.daf.catalogmanager.utilities.{CatalogManager, CoherenceChecker}
  import it.gov.daf.catalogmanager.utilities.uri.UriDataset
  import play.Environment
  import play.api.libs.json._
  import play.api.libs.ws.WSClient
  //import it.gov.daf.catalogmanager.utilities.datastructures._
  import play.api.Logger
  import scala.util.Failure
  import scala.util.Try
  import catalog_manager.yaml.Success
  import scala.concurrent.Future


  /**
    * Created by ale on 05/05/17.
    */

  class CatalogRepositoryFile extends CatalogRepository{

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


    private val streamDcat =
      new FileInputStream(Environment.simple().getFile("data/data-mgt/data-dcatapit.json"))
    private val dcatSchema: JsValue = try {
      Json.parse(streamDcat)
    } finally {
      streamDcat.close()
    }

    private val streamMetaCatalog =
      new FileInputStream(Environment.simple().getFile("data/data-mgt/data_test.json"))
    private val metaSchema: JsValue = try {
      Json.parse(streamMetaCatalog)
    } finally {
      streamMetaCatalog.close()
    }

    //println(Json.stringify(metaSchema))

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


    val dcatJson: JsResult[Dataset] = dcatSchema.validate[Dataset]
    val dcat = dcatJson match {
      case s: JsSuccess[Dataset] => Option(s.get)
      case e: JsError => None
    }

    def listCatalogs(page :Option[Int], limit :Option[Int]) :Seq[MetaCatalog] = {
      val file: File = Environment.simple().getFile("data/data-mgt/data_test.json")
      val lines = scala.io.Source.fromFile(file).getLines()

      val results= lines.map(line => {
        val metaCatalogJs = Json.parse(line)
        val metaCatalogResult: JsResult[MetaCatalog] = metaCatalogJs.validate[MetaCatalog]
        metaCatalogResult match {
          case s: JsSuccess[MetaCatalog] => Some(s.get)
          case e: JsError => None
        }

      }).filter(x => x.isDefined).map(x=>x.get).toList

      results
    }

    def catalog(catalogId :String): Option[MetaCatalog] = {
      println(catalogId)
      println("####################")
      val file: File = Environment.simple().getFile("data/data-mgt/data_test.json")
      val lines = scala.io.Source.fromFile(file).getLines()
      val results: Seq[Option[MetaCatalog]] = lines.map(line => {
        println(line)
        val metaCatalogJs = Json.parse(line)
        println(metaCatalogJs.toString())
        val metaCatalogResult: JsResult[MetaCatalog] = metaCatalogJs.validate[MetaCatalog]
        metaCatalogResult match {
          case s: JsSuccess[MetaCatalog] => Some(s.get)
          case e: JsError => {
            println("ERRORE qui!!!!!!!!!!!!!")
            println(e)
            None
          }
        }

      }).toList.filter( x =>
        x match {
          case Some(s) => s.operational.logical_uri.equals(catalogId)
          case None => false
        })

      results match {
        case List() => None
        case _ => results(0)
      }
    }


    def catalogByName(name :String, groups: List[String]): Option[MetaCatalog] = {
      println(name)
      println("####################")
      val file: File = Environment.simple().getFile("data/data-mgt/data_test.json")
      val lines = scala.io.Source.fromFile(file).getLines()
      val results: Seq[Option[MetaCatalog]] = lines.map(line => {
        println(line)
        val metaCatalogJs = Json.parse(line)
        println(metaCatalogJs.toString())
        val metaCatalogResult: JsResult[MetaCatalog] = metaCatalogJs.validate[MetaCatalog]
        metaCatalogResult match {
          case s: JsSuccess[MetaCatalog] => Some(s.get)
          case e: JsError => {
            println("ERRORE qui!!!!!!!!!!!!!")
            println(e)
            None
          }
        }

      }).toList.filter( x =>
        x match {
          case Some(s) => s.dcatapit.title.equals(name)
          case None => false
        })

      results match {
        case List() => None
        case _ => results(0)
      }
    }


    def publicCatalogByName(name :String): Option[MetaCatalog] = {
      println(name)
      println("####################")
      val file: File = Environment.simple().getFile("data/data-mgt/data_test.json")
      val lines = scala.io.Source.fromFile(file).getLines()
      val results: Seq[Option[MetaCatalog]] = lines.map(line => {
        println(line)
        val metaCatalogJs = Json.parse(line)
        println(metaCatalogJs.toString())
        val metaCatalogResult: JsResult[MetaCatalog] = metaCatalogJs.validate[MetaCatalog]
        metaCatalogResult match {
          case s: JsSuccess[MetaCatalog] => Some(s.get)
          case e: JsError => {
            println("ERRORE qui!!!!!!!!!!!!!")
            println(e)
            None
          }
        }

      }).toList.filter( x =>
        x match {
          case Some(s) => s.dcatapit.title.equals(name)
          case None => false
        })

      results match {
        case List() => None
        case _ => results(0)
      }
    }

    def createCatalogExtOpenData(metaCatalog: MetaCatalog, callingUserid :MetadataCat, ws :WSClient) :Success = {
      import catalog_manager.yaml.ResponseWrites.MetaCatalogWrites

      val fw = new FileWriter("data/data-mgt/data_test.json", true)
      val metaCatalogJs = Json.toJson(metaCatalog)

      val msg: String = metaCatalog match {
        case MetaCatalog(dataSchema, operational, _) =>
          if(operational.std_schema.isDefined ) {
            val stdUri = operational.std_schema.get.std_uri
            val res: Try[(Option[MetaCatalog])] = Try(catalog(stdUri).get)
              .map(CatalogManager.writeOrdinaryWithStandard(metaCatalog, _))
            res match {
              case scala.util.Success(Some(meta)) =>
                val data = Json.toJson(meta)
                fw.write(Json.stringify(data) + "\n")
                fw.close()
                val msg = meta.operational.logical_uri
                msg
              case _ =>
                val msg = "Error"
                msg
            }
          } else {
            val res: Try[Option[MetaCatalog]]= Try(CatalogManager.writeOrdinary(metaCatalog))
            val msg = res match {
              case scala.util.Success(Some(meta)) =>
                val data = Json.toJson(meta)
                fw.write(Json.stringify(data) + "\n")
                fw.close()
                val msg = meta.operational.logical_uri
                msg
              case _ =>
                val msg = "Error"
                msg
            }
            msg
          }

        case _ =>  val msg = "Error"; msg
      }

      Success(msg,Some(msg))
    }

    def createCatalog(metaCatalog: MetaCatalog, callingUserid :MetadataCat, ws :WSClient) :Success = {
      import catalog_manager.yaml.ResponseWrites.MetaCatalogWrites

      val fw = new FileWriter("data/data-mgt/data_test.json", true)
      val metaCatalogJs = Json.toJson(metaCatalog)

      val msg: String = metaCatalog match {
        case MetaCatalog(dataSchema, operational, _) =>
          if(operational.std_schema.isDefined ) {
            val stdUri = operational.std_schema.get.std_uri
            val res: Try[(Option[MetaCatalog])] = Try(catalog(stdUri).get)
              .map(CatalogManager.writeOrdinaryWithStandard(metaCatalog, _))
            res match {
              case scala.util.Success(Some(meta)) =>
                val data = Json.toJson(meta)
                fw.write(Json.stringify(data) + "\n")
                fw.close()
                val msg = meta.operational.logical_uri
                msg
              case _ =>
                val msg = "Error"
                msg
            }
          } else {
            val res: Try[Option[MetaCatalog]]= Try(CatalogManager.writeOrdinary(metaCatalog))
            val msg = res match {
              case scala.util.Success(Some(meta)) =>
                val data = Json.toJson(meta)
                fw.write(Json.stringify(data) + "\n")
                fw.close()
                val msg = meta.operational.logical_uri
                msg
              case _ =>
                val msg = "Error"
                msg
            }
            msg
          }

        case _ =>  val msg = "Error"; msg
      }

      Success(msg,Some(msg))
    }

    // Not used
     def standardUris(): List[String] = List("ale", "raf")

    def isDatasetOnCatalog(name :String) = None

    def deleteCatalogByName(nameCatalog: String, user: String, isAdmin: Boolean): Either[Error, Success] = {
      Right(Success("delete", None))
    }

  }
