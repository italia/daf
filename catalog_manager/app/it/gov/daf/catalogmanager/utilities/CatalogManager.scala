package it.gov.daf.catalogmanager.utilities

import catalog_manager.yaml.{MetaCatalog, StdSchema}
import it.gov.daf.catalogmanager.utilities.uri.UriDataset
import play.api.Logger
import play.api.libs.json.Json


import scala.util.{Failure, Success, Try}

/**
  * Created by ale on 30/05/17.
  */
object CatalogManager {

  import catalog_manager.yaml.ResponseWrites.MetaCatalogWrites

  def savoAsFile(metaCatalog: MetaCatalog) = println(Json.toJson(metaCatalog))


  // NOT WORKING YET
  def toWrite(metaCatalog :MetaCatalog, standardMeta :Option[MetaCatalog]) :MetaCatalog = {
    val msg: String = metaCatalog match {
      case MetaCatalog(Some(dataSchema), Some(operational), _) =>
        if(operational.std_schema.isDefined ) {
          val stdUri = operational.std_schema.get.std_uri.get
          val res: Try[(Boolean, MetaCatalog)] = Try(writeOrdinaryWithStandard(metaCatalog, standardMeta.get))
          res match {
            case Success((true, meta)) =>
              val random = scala.util.Random
              val id = random.nextInt(1000).toString
              val data = Json.obj(id -> Json.toJson(meta))

              val msg = "Catalog Added"
              msg
            case _ =>
              println("Error");
              val msg = "Error"
              msg
          }
        } else {
          val random = scala.util.Random
          val id = random.nextInt(1000).toString
          val res: Try[(Boolean, MetaCatalog)]= Try(writeOrdinary(metaCatalog))
          val msg = res match {
            case Success((true, meta)) =>
              val random = scala.util.Random
              val id = random.nextInt(1000).toString
              val data = Json.obj(id -> Json.toJson(meta))
             // fw.write(Json.stringify(data) + "\n")
             // fw.close()
              val msg = "Catalog Added"
              msg
            case _ =>
              println("Error");
              val msg = "Error"
              msg
          }
          msg
        }
    }
    MetaCatalog(None,None,None)
  }

  def writeOrdinaryWithStandard(metaCatalogOrdinary: MetaCatalog, metaCatalogStandard: MetaCatalog ): (Boolean, MetaCatalog) = {

    val checkSchema: Try[Boolean] = CoherenceChecker.checkCoherenceSchemas(metaCatalogOrdinary, metaCatalogStandard)

    checkSchema match {
      case Success(value) =>
        Logger.info(s"Storing schema having url ${metaCatalogOrdinary.operational.get.logical_uri}.")

        val toSave: Try[MetaCatalog] = for {
          uriDataset <- UriDataset.convertToUriDataset(metaCatalogOrdinary)
          logicalUri <- Try(uriDataset.getUri())
          physicalUri <- Try(uriDataset.getUrl())
          operational <- Try(metaCatalogOrdinary.operational.get.copy(logical_uri = Some(logicalUri), physical_uri = Some(physicalUri)))
          newSchema <- Try(metaCatalogOrdinary.copy( operational = Some(operational)))
        } yield newSchema

        toSave match {
          case Success(save) =>
            //savoAsFile(save)
            (true, save)
          case Failure(ex) =>
            println(ex.getMessage)
            (false, MetaCatalog(None,None,None))
        }

      case Failure(ex)  =>
        Logger.error(s"Unable to write the schema with uri ${metaCatalogOrdinary.operational.get.logical_uri}. ERROR message: \t ${ex.getMessage} ${ex.getStackTrace.mkString("\n\t")}")
        (false, MetaCatalog(None,None,None))
    }
  }

  def writeOrdinary(metaCatalogOrdinary: MetaCatalog) : (Boolean, MetaCatalog) = {
    val toSave: Try[MetaCatalog] = for {
      uriDataset <- UriDataset.convertToUriDataset(metaCatalogOrdinary)
      logicalUri <- Try(uriDataset.getUri())
      physicalUri <- Try(uriDataset.getUrl())
      operational <- Try(metaCatalogOrdinary.operational.get.copy(logical_uri = Some(logicalUri), physical_uri = Some(physicalUri)))
      newSchema <- Try(metaCatalogOrdinary.copy( operational = Some(operational)))
    } yield newSchema

    toSave match {
      case Success(save) =>
        //savoAsFile(save)
        (true, save)
      case Failure(ex) =>
        println(ex.getMessage)
        (false, MetaCatalog(None,None,None))
  }
  }

  def writeStandard(metaCatalogOrdinary: MetaCatalog) : (Boolean, MetaCatalog) = {
    val toSave: Try[MetaCatalog] = for {
      uriDataset <- UriDataset.convertToUriDataset(metaCatalogOrdinary)
      logicalUri <- Try(uriDataset.getUri())
      physicalUri <- Try(uriDataset.getUrl())
      operational <- Try(metaCatalogOrdinary.operational.get.copy(logical_uri = Some(logicalUri), physical_uri = Some(physicalUri)))
      newSchema <- Try(metaCatalogOrdinary.copy( operational = Some(operational)))
    } yield newSchema

    toSave match {
      case Success(save) =>
        //savoAsFile(save)
        (true, save)
      case Failure(ex) =>
        println(ex.getMessage)
        (false, MetaCatalog(None,None,None))
    }
  }

}
