package it.gov.daf.catalogmanager.repository.ckan

import java.io.{FileInputStream, PrintWriter}

import catalog_manager.yaml.{Dataset, MetadataCat, Organization, ResourceSize}
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

  def createDataset( jsonDataset: JsValue ): Future[String] = try {
    datasetWriter.println(jsonDataset.toString)
    Future("ok")
  } finally {
    datasetWriter.flush()
  }

  def createOrganization( jsonDataset: JsValue ) : Future[String] = {
    Future("todo")
  }


  def dataset(datasetId: String): JsValue = {
    readDataset()
  }

  def getOrganization(orgId :String) : Future[JsResult[Organization]] = {
    Future(null)
  }

  def getOrganizations() : Future[JsValue] = {
    Future(null)
  }

  def getDatasets() : Future[JsValue] = {
    Future(null)
  }

  def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize) ) : Future[JsResult[Seq[Dataset]]]={
    Future(null)
  }

  def getDatasetsWithRes( input: (ResourceSize, ResourceSize) ) : Future[JsResult[Seq[Dataset]]] = {
    Future(null)
  }

  def testDataset(datasetId :String) : Future[JsResult[Dataset]] = {
    Future(JsSuccess(Dataset(None,None,None,None,None,
      None,None,None,None,None,None,None,
      None,None,None,None,None,
      None,None,None,None,None)))
  }

}
