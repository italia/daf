package it.gov.daf.catalogmanager.repository.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import catalog_manager.yaml.{Dataset, MetadataCat, Organization, ResourceSize}
import play.api.libs.ws.ahc.AhcWSClient
import it.gov.daf.catalogmanager.utilities.{ConfigReader, WebServiceUtil}
import play.api.libs.json._

import scala.concurrent.Future

/**
  * Created by ale on 11/05/17.
  */

class CkanRepositoryProd extends CkanRepository{

  import catalog_manager.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private val LOCALURL = "http://localhost:9000"
  private val CKAN_ERROR = "CKAN service is not working correctly"

  def createDataset(jsonDataset: JsValue): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createDataset"
    wsClient.url(url).post(jsonDataset).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def createOrganization(jsonDataset: JsValue): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createOrganization"
    wsClient.url(url).post(jsonDataset).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def dataset(datasetId: String): JsValue = JsString("TODO")


  def getOrganization(orgId :String) : Future[JsResult[Organization]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/organization/" + orgId
    wsClient.url(url).get().map ({ response =>
      val orgJson: JsValue = (response.json \ "result")
        .getOrElse(Json.obj("error" -> "No organization"))
      val orgValidate = orgJson.validate[Organization]
      orgValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getOrganizations() : Future[JsValue] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/organizations"
    wsClient.url(url).get().map ({ response =>
      val orgsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))
      orgsListJson
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }


  def getDatasets() : Future[JsValue] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/datasets"
    wsClient.url(url).get().map ({ response =>
      val dsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))
      dsListJson
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }

  def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize) ) : Future[JsResult[Seq[Dataset]]]={

    val wsClient = AhcWSClient()

    val params = Map(("q",input._1),("sort",input._2),("rows",input._3))

    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    val url =  LOCALURL + "/ckan/searchDataset"+queryString

    wsClient.url(url).get().map ({ response =>
      val datasetJson: JsValue =( (response.json \ "result") \ "results")
        .getOrElse(Json.obj("error" -> "No datasets"))

      val datasetsValidate = datasetJson.validate[Seq[Dataset]]
      println(datasetsValidate)
      datasetsValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getDatasetsWithRes( input: (ResourceSize, ResourceSize) ) : Future[JsResult[Seq[Dataset]]] = {

    val wsClient = AhcWSClient()

    val params = Map( ("limit",input._1),("offset",input._2) )

    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    val url =  LOCALURL + "/ckan/datasetsWithResources"+queryString

    wsClient.url(url).get().map ({ response =>
      val datasetJson: JsValue =(response.json \ "result")
        .getOrElse(Json.obj("error" -> "No datasets"))

      val datasetsValidate = datasetJson.validate[Seq[Dataset]]
      println(datasetsValidate)
      datasetsValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


  def testDataset(datasetId :String) : Future[JsResult[Dataset]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/dataset/" + datasetId

    wsClient.url(url).get().map ({ response =>
      val datasetJson: JsValue = (response.json \ "result")
        .getOrElse(Json.obj("error" -> "No dataset"))
      val datasetValidate = datasetJson.validate[Dataset]
      datasetValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


}

