package it.gov.daf.catalogmanager.repository.ckan

import java.io.FileInputStream

import catalog_manager.yaml.Dataset
import play.Environment
import play.api.libs.json._

import scala.concurrent.Future

/**
  * Created by ale on 10/05/17.
  */
class CkanRepositoryDev extends CkanRepository{

  import scala.concurrent.ExecutionContext.Implicits.global

  private val streamSchema =
    new FileInputStream(Environment.simple().getFile("data/ckan-dataset.json"))
  private val ckanSchema: JsValue = try {
    Json.parse(streamSchema)
  } finally {
    streamSchema.close()
  }

  import catalog_manager.yaml.BodyReads.DatasetReads

  val datasetJson: JsResult[Dataset] = ckanSchema.validate[Dataset]
  val dataset: Option[Dataset] = datasetJson match {
    case s: JsSuccess[Dataset] => println(s.get);Option(s.get)
    case e: JsError => println(e); None;
  }

  def getDataset(datasetId :String) :Future[Dataset] = {
     Future(dataset.getOrElse(Dataset(None,None,None,None,None,None,
       None,None,None,None,None,None,None,None,None,None,None,None,
       None,None,None,None)))
    //Dataset(_)
   // None
  }

}
