package it.gov.daf.ingestion.nifi

import com.typesafe.config.Config
import it.gov.daf.catalogmanager._
import it.gov.daf.ingestion.metacatalog.MetaCatalogProcessor
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsLookupResult, JsValue}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

object NifiProcessor {

  final case class Processor(uid: String, name: String, attributes: Map[String, String])

  final case class NifiResult(
    status: String,
    dataset: String,
    niFiInfo: List[Processor]
  )

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

  def createDataFlow(): Future[List[NifiResult]] = {

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

    Future.sequence(result)
  }

  def createFlow(sftp: SourceSftp): Future[NifiResult] = {
    implicit def unwrap(v: JsLookupResult): String = v.get.toString().replaceAll("\"", "")
    for {
      //create the input and extract fields
      (inputProc, inputWs) <- createProcessor(sftp)
      inputId = inputWs.json \ "id"
      uniqueVal = inputProc.attributes.getOrElse("uniqueVal", "")

      //create update attributes and extract fields
      (attributeProc, attributeWs) <- createAttributeProcessor(inputProc.attributes)
      attributeId = attributeWs.json \ "id"

      //create connections
      updateAttrConn <- createConnection(inputId, attributeId, uniqueVal, "updateAttr")
      funnelConn <- createConnection(attributeId, nifiFunnelId, uniqueVal, "funnel")

      //start processors
      startInput <- starProcessor(inputWs)
      startAttribute <- starProcessor(attributeWs)

    } yield {

      val operations = inputWs :: attributeWs :: updateAttrConn :: funnelConn :: startInput :: startAttribute :: Nil
      val failed = operations.filter(_.status > 207)

      if (failed.isEmpty) NifiResult("200", sftp.name, List(inputProc, attributeProc))
      else {
        //TODO make sequential
        deleteFlow(List(inputWs, attributeWs))
        throw new Throwable(s"Failed to process ${failed.map(_.body)}")
      }
    }
  }

  private def isSuccess(wSResponse: WSResponse) = wSResponse.status < 208

  /**
   *
   * @param nodes the list of the nodes to stop and to delete
   * @return executes all the deletes and return the re
   */
  private def deleteFlow(nodes: List[WSResponse]): Future[String] = {
    Future.sequence(nodes
      .map(r => stopProcessor(r).flatMap(_ => deleteProcessor(r))))
      .flatMap { l =>
        if (l.forall(isSuccess)) Future.successful("Removed all created nodes")
        else Future.failed(new Throwable(s"Error ${l.map(_.body)}"))
      }
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

  def createProcessor(sftp: SourceSftp): Future[(Processor, WSResponse)] = {
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
          .filter(_.status <= 201)
          .map { response =>
            val componentId: String = (response.json \ "component" \ "id").as[String]
            (Processor(name, componentId, params), response)
          }

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
          .filter(_.status <= 201)
          .map { response =>
            val componentId: String = (response.json \ "component" \ "id").as[String]
            (Processor(name, componentId, params), response)
          }

      case other =>
        logger.error("invalid sftp processor definition for {}", other)
        Future.failed(new IllegalArgumentException(s"invalid sftp processor definition for $other"))
    }
  }

  def createAttributeProcessor(params: Map[String, String]): Future[(Processor, WSResponse)] = {
    val json = NifiHelper.updateAttrProcessor(
      clientId = catalogWrapper.dsName + "_updateAttr_" + params("uniqueVal"),
      name = catalogWrapper.dsName + "_updateAttr_" + params("uniqueVal"),
      inputSrc = catalogWrapper.inputSrcNifi(),
      storage = catalogWrapper.storageNifi(),
      dataschema = catalogWrapper.dataschemaNifi(),
      dataset_type = catalogWrapper.dataset_typeNifi(),
      transfPipeline = catalogWrapper.ingPipelineNifi(),
      format = catalogWrapper.fileFormatNifi(),
      theme = catalogWrapper.theme(),
      subTheme = catalogWrapper.subtheme(),
      hdfsPath = catalogWrapper.hdfsPath(),
      avroSchema = catalogWrapper.avroSchema(),
      sep = catalogWrapper.separator()
    )

    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
    logger.debug(s"posting processor $json to ${request.url}")
    request.post(json)
      .filter(_.status <= 201)
      .map { response =>
        val componentId: String = (response.json \ "component" \ "id").as[String]
        (Processor(catalogWrapper.dsName + "_updateAttr_" + params("uniqueVal"), componentId, params), response)
      }
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
