package it.gov.daf.catalogmanager.repository.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import catalog_manager.yaml.Dataset
import it.gov.daf.catalogmanager.utils.ConfigReader
import play.api.libs.ws.ahc.AhcWSClient
import it.gov.daf.catalogmanager.utils.WebServiceUtil
import play.api.libs.json.{JsError, JsSuccess, JsValue}

import scala.concurrent.Future

/**
  * Created by ale on 11/05/17.
  */

class CkanRepositoryProd extends CkanRepository{

  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import catalog_manager.yaml.BodyReads.DatasetReads

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val CKAN_URL = ConfigReader.getCkanHost

  def getDataset(datasetId :String) :Future[Dataset] = {

    val url = CKAN_URL + "/api/3/action/package_show?id=" + datasetId
    println("URL " + url)
    val wsClient = AhcWSClient(WebServiceUtil.ahcConfig)
    val test = wsClient.url(url).get
    val result: Future[Dataset] = test map { response =>
      // val bodyResponse :String = response.body
      val datasetJson: JsValue = (response.json \ "result").get
      val datasetValidate = datasetJson.validate[Dataset]
      val dataset: Dataset = datasetValidate match {
        case s: JsSuccess[Dataset] => println(s.get);s.get
        case e: JsError => println(e); Dataset(None,None,None,None,None,
          None,None,None,None,None,None,None,
          None,None,None,None,None,
          None,None,None,None,None)
      }
      wsClient.close()
      dataset
    }
    result
  }

}
