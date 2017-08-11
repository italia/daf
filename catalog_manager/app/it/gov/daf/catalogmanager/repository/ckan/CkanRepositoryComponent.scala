package it.gov.daf.catalogmanager.repository.ckan

import catalog_manager.yaml.{Credentials, User}
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

  def getMongoUser(name:String,callingUserid :MetadataCat): JsResult[User]
  def verifyCredentials(credentials: Credentials):Boolean
  def updateOrganization(orgId: String, jsonOrg: JsValue,callingUserid :MetadataCat): Future[String]
  def createUser(jsonUser: JsValue,callingUserid :MetadataCat): Future[String]
  def getUserOrganizations(userName :String,callingUserid :MetadataCat) : Future[JsResult[Seq[Organization]]]
  def createDataset(jsonDataset: JsValue,callingUserid :MetadataCat): Future[String]
  def createOrganization(jsonDataset: JsValue,callingUserid :MetadataCat): Future[String]
  def dataset(datasetId: String,callingUserid :MetadataCat): JsValue
  def getOrganization(orgId :String,callingUserid :MetadataCat) : Future[JsResult[Organization]]
  def getOrganizations(callingUserid :MetadataCat) : Future[JsValue]
  def getDatasets(callingUserid :MetadataCat) : Future[JsValue]
  def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize), callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]]
  def getDatasetsWithRes( input: (ResourceSize, ResourceSize), callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]]
  def testDataset(datasetId :String, callingUserid :MetadataCat) : Future[JsResult[Dataset]]

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
