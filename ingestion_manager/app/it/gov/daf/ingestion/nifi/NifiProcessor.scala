package it.gov.daf.ingestion.nifi

import com.typesafe.config.Config
import it.gov.daf.catalogmanager._
import it.gov.daf.ingestion.metacatalog.MetaCatalogProcessor
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsLookupResult, JsValue}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object NifiProcessor {

  //dsName è il nome del dataset
  //procList contiene le coppie (Id_del_processore, bool)
  //dove bool è true se il processore è creato e in stato di play, false altrimenti
  /**
    *
    * @param dsName
    * @param procList
    */
  final case class NiFiInfo(dsName: String, procList: List[(String, Boolean)])

  //per ora lo status è: "Ok" e "NOk" che vengono definiti in base a procList
  // => se la lista contine solo (_, true) è "Ok", altrimenti "NOk"
  final case class NiFiProcessStatus(status: String, niFiInfo: NiFiInfo)

  def apply(metaCatalog: MetaCatalog)(implicit ws: WSClient, config: Config, ec: ExecutionContext): NifiProcessor =
    new NifiProcessor(
      metaCatalog,
      ws,
      config.getString("WebServices.nifiUrl"),
      config.getString("ingmgr.niFi.funnelId"),
      config.getString("ingmgr.niFi.groupId"),
      ec
    )
}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.*"
  )
)
class NifiProcessor(
                     metaCatalog: MetaCatalog,
                     ws: WSClient,
                     val nifiUrl: String,
                     val nifiFunnelId: String,
                     val nifiGroupId: String,
                     implicit val ec: ExecutionContext
                   ) {

  import NifiProcessor._

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val catalogWrapper = new MetaCatalogProcessor(metaCatalog)

  private[this] var counter = 1

  private def incCounter() = {
    counter += 1
    counter
  }

  def createDataFlow(): Future[NiFiProcessStatus] = {

    val result = catalogWrapper.inputSrc().sftp
      .getOrElse(List.empty)
      .map(createFlow)

    catalogWrapper.inputSrc().srv_pull
      .getOrElse(List.empty)
      .map(createFlow)

    catalogWrapper.inputSrc().srv_push
      .getOrElse(List.empty)
      .map(createFlow)

    catalogWrapper.inputSrc().daf_dataset
      .getOrElse(List.empty)
      .map(createFlow)

    //FIXME please
    Future.sequence(result)
      .map(_.flatten)
      .map(listWs => {

        NiFiProcessStatus(
          "OK but change me",
          NiFiInfo(catalogWrapper.dsName(), List.empty)
        )
      }
      )
  }


  def createFlow(sftp: SourceSftp): Future[List[WSResponse]] = {
    //    implicit def unwrap(v: JsLookupResult): String = v.get.toString().tail.dropRight(1)
    implicit def unwrap(v: JsLookupResult): String = v.get.toString().replaceAll("\"", "")

    for {
      //create the input and extract fields
      (inputJson, inputParams, inputWs) <- createProcessor(sftp)
      inputId = inputWs.json \ "id"
      uniqueVal = inputParams.getOrElse("uniqueVal", "")

      //create update attributes and extract fields
      (_, _, attributeWs) <- createAttributeProcessor(inputParams)
      attributeId = attributeWs.json \ "id"

      //create connections
      updateAttrConn <- createConnection(inputId, attributeId, uniqueVal, "updateAttr")
      funnelConn <- createConnection(attributeId, nifiFunnelId, uniqueVal, "funnel")

      startedInput <- starProcessor(inputWs)
      startedAttribute <- starProcessor(attributeWs)

    } yield {

      println(s"start processor WSResponse: $startedInput")
      println(s"start attribute WSResponse: $startedAttribute")

      if (statusOperazioni(List(inputWs, attributeWs, updateAttrConn, funnelConn, startedInput, startedAttribute))) {
        if (startedInput.status == 200) {
          stopProcessor(inputWs)
        }
        if (startedAttribute.status == 200) {
          stopProcessor(attributeWs)
        }
        val res1 = deleteProcessor(inputWs)
        val res2 = deleteProcessor(attributeWs)

        println(s"response delete procList: $res1")
        println(s"response delete updateAttr: $res2")
      }
      List(inputWs, attributeWs, updateAttrConn, funnelConn, startedInput, startedAttribute)
    }
  }

  def statusOperazioni(list: List[WSResponse]): Boolean = {
    //FIXME
    list.foreach(r => {
      val status = r.status
      if (status != 200 && status != 201) return true
    })
    false
  }

  def deleteProcessor(response: WSResponse): Future[WSResponse] = {
    implicit def unwrap(v: JsLookupResult): String = v.get.toString().replaceAll("\"", "")

    val componentId: String = response.json \ "component" \ "id"
    val clientId: String = response.json \ "revision" \ "clientId"
    val version: String = response.json \ "revision" \ "version"

    val request = ws.url(nifiUrl + "processors/" + componentId + "?version=" + version + "&clientId=" + clientId)
    logger.debug(s"delete processor $componentId to ${request.url}")
    request.delete
  }


  def stopProcessor(response: WSResponse) = {
    implicit def unwrap(v: JsLookupResult): String = v.get.toString().replaceAll("\"", "")

    val componentId: String = response.json \ "component" \ "id"

    val json = NifiHelper.stopProcessor(
      clientId = response.json \ "revision" \ "clientId",
      version = response.json \ "revision" \ "version",
      componentId = componentId
    )

    val request: WSRequest = ws.url(nifiUrl + "processors/" + componentId)
    logger.debug(s"putting processor $json to ${request.url}")
    request.put(json)
  }

  def starProcessor(response: WSResponse) = {
    implicit def unwrap(v: JsLookupResult): String = v.get.toString().replaceAll("\"", "")

    val componentId: String = response.json \ "component" \ "id"

    val json = NifiHelper.runProcessor(
      clientId = response.json \ "revision" \ "clientId",
      version = response.json \ "revision" \ "version",
      componentId = componentId
    )

    val request: WSRequest = ws.url(nifiUrl + "processors/" + componentId)
    logger.debug(s"putting processor $json to ${request.url}")
    request.put(json)
  }

  def createFlow(pull: SourceSrvPull): Future[String] =
    Future.failed(throw new NotImplementedError)

  def createFlow(push: SourceSrvPush): Future[String] = Future.failed(throw new NotImplementedError)

  def createFlow(dataset: SourceDafDataset): Future[String] = Future.failed(throw new NotImplementedError)

  def createProcessor(sftp: SourceSftp): Future[(JsValue, Map[String, String], WSResponse)] = {
    val uniqueVal = incCounter()
    val name = catalogWrapper.dsName + "_sftp_" + uniqueVal

    sftp match {
      case SourceSftp("sftp_daf", None, None, None, _) =>
        //internal DAF path
        val json = NifiHelper.listFileProcessor(
          name = name,
          inputDir = catalogWrapper.sourceSftpPathDefault()
        )

        val params: Map[String, String] = Map(
          "inputType" -> "local",
          "inputDir" -> catalogWrapper.sourceSftpPathDefault(),
          "inputDirLocation" -> "Local",
          "clientId" -> name,
          "name" -> name,
          "user" -> "",
          "pass" -> "",
          "uniqueVal" -> uniqueVal.toString
        )

        val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
        logger.debug(s"posting processor $json to ${request.url}")
        request.post(json)
          .map(res => (json, params, res))

      case SourceSftp(ext, Some(remoteUrl), Some(user), Some(pwd), _) =>
        //external sftp
        //remoteUrl is like sftp://inputPathLocation
        val inputPathLocation = remoteUrl.split("://")(1)

        val json = NifiHelper.listSftpProcessor(
          name = name,
          hostname = inputPathLocation,
          user = user,
          pass = pwd
        )

        val params: Map[String, String] = Map(
          "inputType" -> "sftp",
          "inputDir" -> catalogWrapper.sourceSftpPathDefault(),
          "inputDirLocation" -> inputPathLocation,
          "clientId" -> name,
          "name" -> name,
          "user" -> user,
          "pass" -> pwd,
          "uniqueVal" -> uniqueVal.toString
        )

        val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
        logger.debug(s"posting processor $json to ${request.url}")
        request.post(json)
          .map(res => (json, params, res))

      case other =>
        logger.error("invalid sftp processor definition for {}", other)
        Future.failed(new IllegalArgumentException(s"invalid sftp processor definition for $other"))
    }
  }

  def createAttributeProcessor(params: Map[String, String]): Future[(JsValue, Map[String, String], WSResponse)] = {
    val json = NifiHelper.updateAttrProcessor(
      clientId = catalogWrapper.dsName + "_updateAttr_" + params("uniqueVal"),
      name = catalogWrapper.dsName + "_updateAttr_" + params("uniqueVal"),
      inputSrc = catalogWrapper.inputSrcNifi(),
      storage = catalogWrapper.storageNifi(),
      dataschema = catalogWrapper.dataschemaNifi(),
      dataset_type = catalogWrapper.dataset_typeNifi(),
      transfPipeline = catalogWrapper.ingPipelineNifi(),
      format = catalogWrapper.fileFormatNifi()
    )

    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
    logger.debug(s"posting processor $json to ${request.url}")
    request.post(json)
      .map(res => (json, params, res))
  }

  def createConnection(
                        inputId: String,
                        attributeId: String,
                        uniqueVal: String,
                        connName: String
                      ): Future[WSResponse] = {
    val name = s"${catalogWrapper.dsName()}_${connName}_$uniqueVal"
    val json = NifiHelper.defineConnection(
      clientId = name,
      name = name,
      sourceId = inputId,
      sourceGroupId = nifiGroupId,
      sourceType = "PROCESSOR",
      destId = attributeId,
      destGroupId = nifiGroupId,
      destType = "PROCESSOR"
    )

    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/connections")
    logger.debug(s"posting connection $json to ${request.url}")
    request.post(json)
  }
}
