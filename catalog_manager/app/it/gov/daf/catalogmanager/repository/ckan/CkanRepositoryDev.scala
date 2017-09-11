package it.gov.daf.catalogmanager.repository.ckan

import java.io.{FileInputStream, PrintWriter}

import catalog_manager.yaml.{Credentials, Dataset, MetadataCat, Organization, ResourceSize, User}
import play.Environment
import play.api.libs.json._

import scala.concurrent.Future

/**
  * Created by ale on 10/05/17.
  */
class CkanRepositoryDev extends CkanRepository{


  import scala.concurrent.ExecutionContext.Implicits.global

  private def readDataset():JsValue = {
    val streamDataset = new FileInputStream(Environment.simple().getFile("data/Dataset.json"))
    try {
      Json.parse(streamDataset)
    }catch {
      case tr: Throwable => tr.printStackTrace(); JsString("Empty file")
    }finally {
      streamDataset.close()
    }
  }

  private val datasetWriter = new PrintWriter(Environment.simple().getFile("data/Dataset.json"))

  def createDataset( jsonDataset: JsValue, callingUserid :MetadataCat ): Future[String] = try {
    datasetWriter.println(jsonDataset.toString)
    Future("ok")
  } finally {
    datasetWriter.flush()
  }

  def getMongoUser(name:String,callingUserid :MetadataCat): JsResult[User]={
    JsSuccess(null)
  }

  def verifyCredentials(credentials: Credentials):Boolean = {
    true
  }

  def updateOrganization(orgId: String, jsonOrg: JsValue, callingUserid :MetadataCat): Future[String] = {
    Future("todo")
  }

  def patchOrganization(orgId: String, jsonOrg: JsValue, callingUserid :MetadataCat): Future[String] = {
    Future("todo")
  }

  def createOrganization( jsonDataset: JsValue, callingUserid :MetadataCat ) : Future[String] = {
    Future("todo")
  }

  def createUser(jsonUser: JsValue, callingUserid :MetadataCat): Future[String]= {
    Future("todo")
  }

  def getUserOrganizations(userName :String, callingUserid :MetadataCat) : Future[JsResult[Seq[Organization]]] = {
    Future(null)
  }


  def dataset(datasetId: String, callingUserid :MetadataCat): JsValue = {
    readDataset()
  }

  def getOrganization(orgId :String, callingUserid :MetadataCat) : Future[JsResult[Organization]] = {
    Future(null)
  }

  def getOrganizations(callingUserid :MetadataCat) : Future[JsValue] = {
    Future(null)
  }

  def getDatasets(callingUserid :MetadataCat) : Future[JsValue] = {
    Future(null)
  }

  def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize), callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]]={
    Future(null)
  }

  def getDatasetsWithRes( input: (ResourceSize, ResourceSize), callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]] = {
    Future(null)
  }

  def testDataset(datasetId :String, callingUserid :MetadataCat) : Future[JsResult[Dataset]] = {
    Future(null)
    /*
    Future(JsSuccess(Dataset(None,None,None,None,None,
      None,None,None,None,None,None,None,
      None,None,None,None,None,
      None,None,None,None,None)))*/
  }

}
