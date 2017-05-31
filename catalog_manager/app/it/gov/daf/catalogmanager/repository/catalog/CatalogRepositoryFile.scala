  package it.gov.daf.catalogmanager.repository.catalog

  /**
    * Created by ale on 23/05/17.
    */

  import java.io.{FileInputStream, FileWriter}

  import catalog_manager.yaml._
  import it.gov.daf.catalogmanager.utilities.{CatalogManager, CoherenceChecker}
  import it.gov.daf.catalogmanager.utilities.uri.UriDataset
  import play.Environment
  import play.api.libs.json._
  import it.gov.daf.catalogmanager.utilities.datastructures._
  import play.api.Logger

  import scala.util.Success
  import scala.util.Failure
  import scala.util.Try


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
      new FileInputStream(Environment.simple().getFile("data/data-mgt/data.json"))
    private val metaSchema: JsValue = try {
      Json.parse(streamMetaCatalog)
    } finally {
      streamMetaCatalog.close()
    }

    println(Json.stringify(metaSchema))

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
      val metaCatalogResult: JsResult[MetaCatalog] = (metaSchema \ catalogId).validate[MetaCatalog]
      metaCatalogResult match {
        case s: JsSuccess[MetaCatalog] => s.get
        case e: JsError => MetaCatalog(None,None,None)
      }
    }


    def createCatalog(metaCatalog: MetaCatalog) :Successf = {
      import catalog_manager.yaml.ResponseWrites.MetaCatalogWrites
      val fw = new FileWriter("data/data-mgt/data.json", true)
      val metaCatalogJs = Json.toJson(metaCatalog)
      println(Json.stringify(metaCatalogJs))

      metaCatalog match {
        case MetaCatalog(Some(dataSchema), Some(operational), _) =>
          if(operational.std_schema.isDefined ) {
            val stdUri = operational.std_schema.get.std_uri.get

            val res: Try[Boolean] = Try(getCatalogs(stdUri)).map(CatalogManager.writeOrdinaryWithStandard(metaCatalog, _))

          } else {

          }

        case _ => println("")
      }

      val random = scala.util.Random
      val id = random.nextInt(1000).toString
      val data = Json.obj(id -> metaCatalogJs)
    //  fw.write(Json.stringify(data) + "\n")
    //  fw.close()
      Successf(Some("Metacatalog added"),Some("Metacatalog added"))
    }
  }
