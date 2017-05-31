package it.gov.daf.catalogmanager.utilities

import catalog_manager.yaml.{MetaCatalog, StdSchema}
import it.gov.daf.catalogmanager.utilities.datastructures.{ConvSchema, convertToConvSchema, convertToStdSchema}
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

  def writeOrdinaryWithStandard(metaCatalogOrdinary: MetaCatalog, metaCatalogStandard: MetaCatalog ): Boolean = {

    val checkSchema = for {
      convSchema <- convertToConvSchema(metaCatalogOrdinary)
      stdSchema <- convertToStdSchema(metaCatalogStandard)
    } yield CoherenceChecker.checkCoherenceSchemas(convSchema, stdSchema)

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
            savoAsFile(save)
            true
          case Failure(ex) =>
            println(ex.getMessage)
            false
        }
      //value
      case Failure(ex)  =>
        Logger.error(s"Unable to write the schema with uri ${metaCatalogOrdinary.operational.get.logical_uri}. ERROR message: \t ${ex.getMessage} ${ex.getStackTrace.mkString("\n\t")}")
        false
    }
  }

  def writeOrdinary(metaCatalogOrdinary: MetaCatalog) : Boolean = {
    val toSave: Try[MetaCatalog] = for {
      uriDataset <- UriDataset.convertToUriDataset(metaCatalogOrdinary)
      logicalUri <- Try(uriDataset.getUri())
      physicalUri <- Try(uriDataset.getUrl())
      operational <- Try(metaCatalogOrdinary.operational.get.copy(logical_uri = Some(logicalUri), physical_uri = Some(physicalUri)))
      newSchema <- Try(metaCatalogOrdinary.copy( operational = Some(operational)))
    } yield newSchema

    toSave match {
      case Success(save) =>
        savoAsFile(save)
        true
      case Failure(ex) =>
        println(ex.getMessage)
        false
  }
  }

  def writeStandard(metaCatalogOrdinary: MetaCatalog) : Boolean = {
    val toSave: Try[MetaCatalog] = for {
      uriDataset <- UriDataset.convertToUriDataset(metaCatalogOrdinary)
      logicalUri <- Try(uriDataset.getUri())
      physicalUri <- Try(uriDataset.getUrl())
      operational <- Try(metaCatalogOrdinary.operational.get.copy(logical_uri = Some(logicalUri), physical_uri = Some(physicalUri)))
      newSchema <- Try(metaCatalogOrdinary.copy( operational = Some(operational)))
    } yield newSchema

    toSave match {
      case Success(save) =>
        savoAsFile(save)
        true
      case Failure(ex) =>
        println(ex.getMessage)
        false}
  }

}
