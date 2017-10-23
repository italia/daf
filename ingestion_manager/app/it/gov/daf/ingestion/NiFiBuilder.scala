package it.gov.daf.ingestion

import com.typesafe.config.ConfigFactory
import it.gov.daf.catalogmanager.{GroupAccess, InputSrc, MetaCatalog, StorageInfo}
import play.api.libs.ws.{WS, WSClient, WSRequest, WSResponse}
import javax.inject.Inject

import it.gov.daf.ingestion.utilities.NifiJson
import play.api.libs.json._

import scala.concurrent.{Await, Future}
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

//class NiFiBuilder(metaCatalogProcessor: MetaCatalogProcessor) {

class NiFiBuilder @Inject() (ws: WSClient,
                             metaCatalog: MetaCatalog) {

  val nifiUrl = ConfigFactory.load().getString("WebServices.nifiUrl")
  val nifiFunnelId = ConfigFactory.load().getString("WebServices.nifiFunnelId")

  val dsName = metaCatalog.dcatapit.name
  val inputSrc: InputSrc = metaCatalog.operational.input_src
  val storage: Option[StorageInfo] = metaCatalog.operational.storage_info
  val ingPipeline: Option[List[String]] = metaCatalog.operational.ingestion_pipeline
  val dsType: String = metaCatalog.operational.dataset_type
  val groupAccess: Option[List[GroupAccess]] = metaCatalog.operational.group_access

  def getNiFiInfo(): NiFiInfo = {

    NiFiInfo(dsName)
  }


  def processorBuilder(): NiFiProcessStatus = {
    val niFiInfo: NiFiInfo = getNiFiInfo()
    //Call NiFi API to setup a new processor

    val futureProcSetup: Future[WSResponse] = for {
      resListener <- processorListener()

      resUpdateAttr <- {
        println("ProcListener -> " + resListener)
        processorUpdateAttr()
      }

      resConnListAttr <- {
        println("ProcUpdateAttr -> " + resUpdateAttr)
        connListAttr((resListener.json \ "id").as[String], (resUpdateAttr.json \ "id").as[String])
      }

      resConnFunnel <- {
        println("ConnListAttr -> " + resConnListAttr)
        connFunnel((resUpdateAttr.json \ "id").as[String], nifiFunnelId)
      }

      resPlayListener <- {
        println("resConnFunnel -> " + resConnFunnel)
        playProc((resListener.json \ "id").as[String],
          "RUNNING",
          (resListener.json \"revision" \ "clientId").as[String],
          (resListener.json \"revision" \ "version").as[Int].toString)
      }

      resPlayUpdateAttr <- {
        println("resPlayListener -> " + resPlayListener)
        playProc((resUpdateAttr.json \ "id").as[String],
          "RUNNING",
          (resUpdateAttr.json \"revision" \ "clientId").as[String],
          (resUpdateAttr.json \"revision" \ "version").as[Int].toString)
      }

    } yield (resPlayUpdateAttr)

    val result: Try[WSResponse] = Await.ready(futureProcSetup, Duration.Inf).value.get

    result match {
      case Success(s) =>
        println("Connection -> " + s)
        NiFiProcessStatus(niFiInfo)
      case Failure(e) =>
        println(e.printStackTrace())
        NiFiProcessStatus(niFiInfo)
    }

  /*
    resultProcListener match {
      case Success(s) =>
        println("ProcListener -> " + s)
        val idProcListener: String = (s.json \ "id").as[String]
        val futureProcUpdateAttr: Future[WSResponse] = processorUpdateAttr()
        val resultProcUpdateAttr: Try[WSResponse] = Await.ready(futureProcUpdateAttr, Duration.Inf).value.get
        resultProcUpdateAttr match {
          case Success(s) =>
            println("ProcUpdateAttr -> " + s.json)
            val idProcUpdateAttrr: String = (s.json \ "id").as[String]
          case Failure(e) => println(e.getStackTrace)
        }
      case Failure(e) => println(e.getStackTrace)
    }
*/

  }

  def processorListener(): Future[WSResponse] = {
    val json: JsValue = NifiJson.listenerProc(
      clientId = "test1",
      name = "test1",
      inputDir = "/home/davide/test"
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    val futureResponse: Future[WSResponse] = request.post(json)

    /*
    val result: Try[WSResponse] = Await.ready(futureResponse, Duration.Inf).value.get
    result match {
      case Success(s) => println(s)
      case Failure(e) => println(e.getStackTrace)
    }
*/
    futureResponse
  }

  def processorUpdateAttr(): Future[WSResponse] = {
    val json: JsValue = NifiJson.updateAttrProc(
      clientId = "testUpdate1",
      name = "testUpdate1",
      //storage = """"{"storage": "storage_val"}"""",
      storage = """"storage_val"""",
      //dataschema = """"[{"name": "name_val", "format": "format_val"}]"""",
      dataschema = """"format_val"""",
      dataset_type = "ordinary",
      //transfPipeline= """"{"transfPipeline": "transfPipeline_val"}"""",
      transfPipeline= """"transfPipeline_val"""",
      format = "csv",
      sep = ";"
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def connListAttr(idListener: String, idUpdateAttr: String): Future[WSResponse] = {
    val json: JsValue = NifiJson.listAttrConn(
      clientId = "testConnListAttr1",
      name = "testConnListAttr1",
      sourceId = idListener,
      sourceGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      sourceType = "PROCESSOR",
      destId = idUpdateAttr,
      destGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      destType = "PROCESSOR"
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    println(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def connFunnel(idUpdateAttr: String, idFunnel: String): Future[WSResponse] = {
    val json: JsValue = NifiJson.listAttrConn(
      clientId = "testConnFunnel",
      name = "testConnFunnel",
      sourceId = idUpdateAttr,
      sourceGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      sourceType = "PROCESSOR",
      destId = idFunnel,
      destGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      destType = "PROCESSOR"
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    println(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def playProc(componentId: String,
               componentState: String,
               clientId: String,
               version: String): Future[WSResponse] = {

    val json: JsValue = NifiJson.playProc(
      clientId = clientId,
      componentId = componentId,
      componentState = componentState,
      version = version
    )

    println(nifiUrl+"processors/" + componentId)
    println(json.toString)

    val request: WSRequest = ws.url(nifiUrl+"processors/" + componentId)

    val futureResponse: Future[WSResponse] = request.put(json)

    futureResponse
  }

//  PUT http://edge1:9090/nifi-api/processors/6f7b46a2-aa15-139f-3083-addf34976b6e

}

case class NiFiInfo(dsName: String)
case class NiFiProcessStatus(niFiInfo: NiFiInfo)
