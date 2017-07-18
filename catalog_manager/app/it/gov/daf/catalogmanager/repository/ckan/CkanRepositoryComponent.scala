package it.gov.daf.catalogmanager.repository.ckan

import play.api.libs.json.JsValue

import scala.concurrent.Future

/**
  * Created by ale on 10/05/17.
  */

import catalog_manager.yaml.{Dataset, MetadataCat, Organization, ResourceSize}
import play.api.libs.json.{JsResult, JsValue}

import scala.concurrent.Future

/**
  * Created by ale on 01/07/17.
  */
trait CkanRepository {

  def createDataset(jsonDataset: JsValue): Future[String]
  def createOrganization(jsonDataset: JsValue): Future[String]
  def dataset(datasetId: String): JsValue
  def getOrganization(orgId :String) : Future[JsResult[Organization]]
  def getOrganizations() : Future[JsValue]
  def getDatasets() : Future[JsValue]
  def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize) ) : Future[JsResult[Seq[Dataset]]]
  def getDatasetsWithRes( input: (ResourceSize, ResourceSize) ) : Future[JsResult[Seq[Dataset]]]
  def testDataset(datasetId :String) : Future[JsResult[Dataset]]

}

object CkanRepository {
  def apply(config: String): CkanRepository = config match {
    case "dev" => new CkanRepositoryDev
    case "prod" => new CkanRepositoryProd
  }
}

trait CkanRepositoryComponent {
  val ckanRepository :CkanRepository
}
