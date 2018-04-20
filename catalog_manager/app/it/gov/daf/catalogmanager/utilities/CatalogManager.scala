package it.gov.daf.catalogmanager.utilities

import catalog_manager.yaml.{MetaCatalog, StdSchema}
import it.gov.daf.catalogmanager.utilities.uri.UriDataset
import it.gov.daf.common.utils.{DafUriConverter, OpenData, Ordinary, Standard}
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
  def toWrite(metaCatalog :MetaCatalog, standardMeta :Option[MetaCatalog]): Option[MetaCatalog] = {
    val msg: String = metaCatalog match {
      case MetaCatalog(dataSchema, operational, _) =>
        if(operational.std_schema.isDefined ) {
          val stdUri = operational.std_schema.get.std_uri
          val res: Option[MetaCatalog] = writeOrdinaryWithStandard(metaCatalog, standardMeta.get)
          res.map{catalog =>
            val random = scala.util.Random
            val id = random.nextInt(1000).toString
            val data = Json.obj(id -> Json.toJson(catalog))
            val msg = "Catalog Added"
            msg

          }.getOrElse("Errore Errore vattacculc")


        } else {
          val random = scala.util.Random
          val id = random.nextInt(1000).toString
          val res: Option[MetaCatalog]= writeOrdinary(metaCatalog)
          res.map{catalog =>
            val random = scala.util.Random
            val id = random.nextInt(1000).toString
            val data = Json.obj(id -> Json.toJson(catalog))
            // fw.write(Json.stringify(data) + "\n")
            // fw.close()
            val msg = "Catalog Added"
            msg
          }.getOrElse("Errore")
        }
    }

    None
  }

  def writeOrdinaryWithStandard(metaCatalogOrdinary: MetaCatalog, metaCatalogStandard: MetaCatalog): Option[MetaCatalog] = {

    val checkSchema: Try[Boolean] = CoherenceChecker.checkCoherenceSchemas(metaCatalogOrdinary, metaCatalogStandard)

    checkSchema match {
      case Success(value) =>
        Logger.info(s"Storing schema having url ${metaCatalogOrdinary.operational.logical_uri}.")

        val toSave: Try[MetaCatalog] = for {
          uriDataset <- Try(UriDataset.convertToUriDataset((metaCatalogOrdinary)))
          logicalUri <- Try(uriDataset.getUri())
          physicalUri <- Try(uriDataset.getUrl())
          operational <- Try(metaCatalogOrdinary.operational.copy(logical_uri = logicalUri, physical_uri = Some(physicalUri)))
          newSchema <- Try(metaCatalogOrdinary.copy( operational = operational))
        } yield newSchema

        toSave match {
          case Success(save) =>
            //savoAsFile(save)
            Some(save)
          case Failure(ex) =>
            println(ex.getMessage)
            None
        }

      case Failure(ex)  =>
        Logger.error(s"Unable to write the schema with uri ${metaCatalogOrdinary.operational.logical_uri}. ERROR message: \t ${ex.getMessage} ${ex.getStackTrace.mkString("\n\t")}")
        None
    }
  }


  // TODO Old depracated
  def writeOrdinary(metaCatalogOrdinary: MetaCatalog): Option[MetaCatalog] = {


    val toSave: Option[MetaCatalog] = for {
      uriDataset <- Option(UriDataset.convertToUriDataset(metaCatalogOrdinary))
      logicalUri <- Option(uriDataset.getUri())
      physicalUri <- Option(uriDataset.getUrl())
      operational <- Option(metaCatalogOrdinary.operational.copy(logical_uri = logicalUri, physical_uri = Some(physicalUri)))
      newSchema <- Option(metaCatalogOrdinary.copy( operational = operational))
    } yield newSchema
    //savoAsFile(save)
    toSave
  }

  // TODO Old depracated
  def writeStandard(metaCatalogOrdinary: MetaCatalog) : Option[MetaCatalog] = {
    val toSave: Option[MetaCatalog] = for {
      uriDataset <- Option(UriDataset.convertToUriDataset(metaCatalogOrdinary))
      logicalUri <- Option(uriDataset.getUri())
      physicalUri <- Option(uriDataset.getUrl())
      operational <- Option(metaCatalogOrdinary.operational.copy(logical_uri = logicalUri, physical_uri = Some(physicalUri)))
      newSchema <- Option(metaCatalogOrdinary.copy( operational = operational))
    } yield newSchema

    //savoAsFile(save)
    toSave
  }


  def writeOrdAndStd(metaCatalog: MetaCatalog) : Option[MetaCatalog] = {

    val datasetType = if (metaCatalog.operational.is_std)
      Standard
    //else if (!metaCatalog.dcatapit.privatex.getOrElse(false))
    //else if (metaCatalog.dcatapit.owner_org.get.equals("open_data"))
    else if (!metaCatalog.operational.ext_opendata.isEmpty)
      OpenData
    else
      Ordinary

    val datasetConverter = new DafUriConverter(
      datasetType,
      metaCatalog.dcatapit.holder_identifier.get,
      metaCatalog.operational.theme,
      metaCatalog.operational.subtheme,
      metaCatalog.dcatapit.name
    )

    val toSave: Option[MetaCatalog] = for {
      uriDataset <- Option(datasetConverter)
      logicalUri <- Option(uriDataset.toLogicalUri)
      physicalUri <- Option(uriDataset.toPhysicalUri())
      operational <- Option(metaCatalog.operational.copy(logical_uri = logicalUri, physical_uri = Some(physicalUri)))
      newSchema <- Option(metaCatalog.copy( operational = operational))
    } yield newSchema

    //savoAsFile(save)
    toSave
  }

}
