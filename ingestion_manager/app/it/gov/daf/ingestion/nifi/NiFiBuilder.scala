package it.gov.daf.ingestion.nifi

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import it.gov.daf.catalogmanager._
import it.gov.daf.ingestion.metacatalog.MetaCatalogProcessor
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

//class NiFiBuilder(metaCatalogProcessor: MetaCatalogProcessor) {
@SuppressWarnings(
Array(
"org.wartremover.warts.ToString"
)
)
class NiFiBuilder @Inject() (ws: WSClient,
                             metaCatalog: MetaCatalog) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val metaCatalogProc = new MetaCatalogProcessor(metaCatalog)

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

    //Create all Listener Processors --> it will not proceed forward if not all listeners work
    val listenersFuture: Future[List[WSResponse]] = Future.sequence(processorListeners())
    var status: Boolean = false


    listenersFuture.onComplete {
      case Success(listeners) =>
        println("Listeners: " + listeners)
        //create the Update Attr proc, make all the connections and play them
        val futureProcSetup = for {
          //resListener <- processorListener()

          resUpdateAttr <- {
            //println("ProcListener -> " + resListener.toString)
            processorUpdateAttr()
          }

          resConnListAttr <- {
            println("ProcUpdateAttr -> " + resUpdateAttr.toString)
            Future.sequence(listeners.map(x=> connListAttr((x.json \ "id").as[String], (resUpdateAttr.json \ "id").as[String])))
          }

          resConnFunnel <- {
            println("ConnListAttr -> " + resConnListAttr.toString)
            connFunnel((resUpdateAttr.json \ "id").as[String], nifiFunnelId)
          }

          resPlayListener <- {
            println("resConnFunnel -> " + resConnFunnel.toString)
            Future.sequence(listeners.map{x=>
              playProc((x.json \ "id").as[String],
                "RUNNING",
                (x.json \"revision" \ "clientId").as[String],
                (x.json \"revision" \ "version").as[Int].toString)
            })

          }

          resPlayUpdateAttr <- {
            println("resPlayListener -> " + resPlayListener.toString)
            playProc((resUpdateAttr.json \ "id").as[String],
              "RUNNING",
              (resUpdateAttr.json \"revision" \ "clientId").as[String],
              (resUpdateAttr.json \"revision" \ "version").as[Int].toString)
          }

        } yield resPlayUpdateAttr


        val result: Try[WSResponse] = Await.ready(futureProcSetup, Duration.Inf).value.get

        result match {
          case Success(s) =>
            println("Connection -> " + s.toString)
            //NiFiProcessStatus(niFiInfo)
            status = true
          case Failure(e) =>
            println(e.printStackTrace())
            //NiFiProcessStatus(niFiInfo)
            status = false
        }

      case Failure(e) =>
        println(e.printStackTrace)
        logger.error("processorBuilder() Error: listeners not created properly. " + e.printStackTrace)
        //NiFiProcessStatus(niFiInfo)
        status = false
    }

  if (status) {
    NiFiProcessStatus(NiFiInfo("OK"))
  } else {
    NiFiProcessStatus(NiFiInfo("ERROR"))
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



  def processorBuilderBackup(): NiFiProcessStatus = {
    val niFiInfo: NiFiInfo = getNiFiInfo()
    //Call NiFi API to setup a new processor

    //Create all Listener Processors --> it will not proceed forward if not all listeners work
    val listenersFuture: Future[List[WSResponse]] = Future.sequence(processorListeners())

    listenersFuture.onComplete {
      case Success(listeners) =>

    }



    val futureProcSetup: Future[WSResponse] = for {
      resListener <- processorListener()

      resUpdateAttr <- {
        println("ProcListener -> " + resListener.toString)
        processorUpdateAttr()
      }

      resConnListAttr <- {
        println("ProcUpdateAttr -> " + resUpdateAttr.toString)
        connListAttr((resListener.json \ "id").as[String], (resUpdateAttr.json \ "id").as[String])
      }

      resConnFunnel <- {
        println("ConnListAttr -> " + resConnListAttr.toString)
        connFunnel((resUpdateAttr.json \ "id").as[String], nifiFunnelId)
      }

      resPlayListener <- {
        println("resConnFunnel -> " + resConnFunnel.toString)
        playProc((resListener.json \ "id").as[String],
          "RUNNING",
          (resListener.json \"revision" \ "clientId").as[String],
          (resListener.json \"revision" \ "version").as[Int].toString)
      }

      resPlayUpdateAttr <- {
        println("resPlayListener -> " + resPlayListener.toString)
        playProc((resUpdateAttr.json \ "id").as[String],
          "RUNNING",
          (resUpdateAttr.json \"revision" \ "clientId").as[String],
          (resUpdateAttr.json \"revision" \ "version").as[Int].toString)
      }

    } yield (resPlayUpdateAttr)

    val result: Try[WSResponse] = Await.ready(futureProcSetup, Duration.Inf).value.get

    result match {
      case Success(s) =>
        println("Connection -> " + s.toString)
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


  // To be dismissed
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

  def procListenerSftp(sftp: SourceSftp, uniqueVal: String): Future[WSResponse] = {

    val (inputDir, inputDirLocation, user, pass) = sftp match {
      case SourceSftp(name, Some(url), Some(user), Some(pass), _) => (url, "sftp", user, pass)
      case SourceSftp("sftp_daf", None, _, _, _) => (metaCatalogProc.sourceSftpPathDefault("sftp_daf"), "local", "", "")
      case SourceSftp(name, None, _, _, _) =>
        logger.warn("procListenerSftp: setting default sftp with no default sftp name \t SourceSftp: " + sftp.toString + " \t sftp_name: " + name)
        (metaCatalogProc.sourceSftpPathDefault("sftp_daf"), "local", "", "")
      case _ =>
        logger.warn("procListenerSftp: something went wrong with sftp configuration \t SourceSftp: " + sftp.toString)
        (metaCatalogProc.sourceSftpPathDefault("sftp_daf"), "local", "", "")
    }

    //val uniqueVal = scala.util.Random.alphanumeric.take(5).mkString


    val json: JsValue = NifiJson.listenerProc(
      clientId = metaCatalogProc.dsName + "_sftp_" + uniqueVal,
      name = metaCatalogProc.dsName + "_sftp_" + uniqueVal,
      inputDir = inputDir,
      inputDirLocation = inputDirLocation,
      user = user,
      pass = pass
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println("procListenerSftp - URL: " + nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println("procListenerSftp - json: " + json)
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def procListenerSrvPull(srvInfo: SourceSrvPull, srvType: String, uniqueVal: String): Future[WSResponse] = {

    val (inputDirIn: String, inputDirLocationIn: String, userIn: String, passIn: String, tokenIn: String) = srvInfo match {
      case SourceSrvPull(name, url, Some(user), Some(pass), Some(token), _) => (url, srvType, user, pass, token)
      case SourceSrvPull(name, url, Some(user), Some(pass), None, _) => (url, srvType, user, pass, "")
      case SourceSrvPull(name, url, None, None, token, _) => (url, srvType, "", "", token)
      case _ =>
        logger.warn("procListenerSrvPull: something went wrong with Srv configuration \t SourceSrvPull: " + srvInfo.toString)
        ("error", srvType, "error", "error", "error")
    }


    val json: JsValue = NifiJson.listenerProc(
      clientId = metaCatalogProc.dsName + "_sftp_" + uniqueVal,
      name = metaCatalogProc.dsName + "_sftp_" + uniqueVal,
      inputDir = inputDirIn,
      inputDirLocation = inputDirLocationIn,
      user = userIn,
      pass = passIn,
      token = tokenIn
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def procListenerSrvPush(srvInfo: SourceSrvPush, srvType: String, uniqueVal: String): Future[WSResponse] = {

    val (inputDirIn: String, inputDirLocationIn: String, userIn: String, passIn: String, tokenIn: String) = srvInfo match {
      case SourceSrvPush(name, url, Some(user), Some(pass), Some(token), _) => (url, srvType, user, pass, token)
      case SourceSrvPush(name, url, Some(user), Some(pass), None, _) => (url, srvType, user, pass, "")
      case SourceSrvPush(name, url, None, None, token, _) => (url, srvType, "", "", token)
      case _ =>
        logger.warn("procListenerSrvPush: something went wrong with Srv configuration \t SourceSrvPush: " + srvInfo.toString)
        ("error", srvType, "error", "error", "error")
    }


    val json: JsValue = NifiJson.listenerProc(
      clientId = metaCatalogProc.dsName + "_sftp_" + uniqueVal,
      name = metaCatalogProc.dsName + "_sftp_" + uniqueVal,
      inputDir = inputDirIn,
      inputDirLocation = inputDirLocationIn,
      user = userIn,
      pass = passIn,
      token = tokenIn
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  //TODO - to be implemented
  def procListenerDafDs(): List[Future[WSResponse]] = {
    List()
  }

  def processorListeners(): List[Future[WSResponse]] = {
    val inputSrc: InputSrc = metaCatalogProc.inputSrc()

    var sftpCount: Int = 0
    val inputSftpList: List[Future[WSResponse]] = inputSrc.sftp match {
      case Some(s) =>
        println("procListeners - Sftp: " + s.toString)
        sftpCount += 1
        s.map(x => procListenerSftp(x, sftpCount.toString))
      case None => List()
    }

    var srvPullCount: Int = 0
    val inputSrvPullList: List[Future[WSResponse]] = inputSrc.srv_pull match {
      case Some(s) =>
        println("procListeners - SrvPull: " + s.toString)
        srvPullCount += 1
        s.map(x => procListenerSrvPull(x, "SrvPull", srvPullCount.toString))
      case None => List()
    }

    var srvPushCount: Int = 0
    val inputSrvPushList: List[Future[WSResponse]] = inputSrc.srv_push match {
      case Some(s) =>
        println("procListeners - SrvPush: " + s.toString)
        srvPushCount += 1
        s.map(x => procListenerSrvPush(x, "SrvPush", srvPushCount.toString))
      case None => List()
    }

    var dafDsCount: Int = 0
    val inputDafDsList: List[Future[WSResponse]] = inputSrc.daf_dataset match {
      case Some(s) =>
        println("procListeners - DafDs: " + s.toString)
        dafDsCount += 1
        //s.map(x => procListenerDafDs())
        List()
      case None => List()
    }

    inputSftpList ++ inputSrvPullList ++ inputSrvPushList ++ inputDafDsList

  }



  def processorUpdateAttr(uniqueVal: String = ""): Future[WSResponse] = {
    val json: JsValue = NifiJson.updateAttrProc(
      clientId = metaCatalogProc.dsName + "_updateAttr_" + uniqueVal,
      name = metaCatalogProc.dsName + "_updateAttr_" + uniqueVal,
      inputSrc = metaCatalogProc.inputSrcNifi(),
      storage = metaCatalogProc.storageNifi(),
      dataschema = metaCatalogProc.dataschemaNifi(),
      dataset_type = metaCatalogProc.dataset_typeNifi(),
      transfPipeline= metaCatalogProc.ingPipelineNifi(),
      format = metaCatalogProc.fileFormatNifi()
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println("processorUpdateAttr - url: " + nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def connListAttr(idListener: String, idUpdateAttr: String, uniqueVal: String = ""): Future[WSResponse] = {
    val json: JsValue = NifiJson.listAttrConn(
      clientId = metaCatalogProc.dsName + "_connListAttr_" + uniqueVal,
      name = metaCatalogProc.dsName + "_connListAttr_" + uniqueVal,
      sourceId = idListener,
      sourceGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      sourceType = "PROCESSOR",
      destId = idUpdateAttr,
      destGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      destType = "PROCESSOR"
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    println("connListAttr - url: " + nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def connFunnel(idUpdateAttr: String, idFunnel: String): Future[WSResponse] = {
    val json: JsValue = NifiJson.listAttrConn(
      clientId = metaCatalogProc.dsName + "_updateAttr",
      name = metaCatalogProc.dsName + "_updateAttr",
      sourceId = idUpdateAttr,
      sourceGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      sourceType = "PROCESSOR",
      destId = idFunnel,
      destGroupId = "6f7b46a2-aa15-139f-3083-addf34976b6e",
      destType = "PROCESSOR"
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    println("connFunnel - url: " + nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/connections")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

  def playProc(componentId: String,
               componentState: String,
               clientId: String,
               version: String): Future[WSResponse] = {

    val json: JsValue = NifiJson.playProc(
      clientId = clientId + "_play",
      componentId = componentId,
      componentState = componentState,
      version = version
    )

    println("playProc - Url: " + nifiUrl+"processors/" + componentId)
    println("playProc - json: " + json.toString)

    val request: WSRequest = ws.url(nifiUrl+"processors/" + componentId)

    val futureResponse: Future[WSResponse] = request.put(json)

    futureResponse
  }

//  PUT http://edge1:9090/nifi-api/processors/6f7b46a2-aa15-139f-3083-addf34976b6e

}

final case class NiFiInfo(message: String)
final case class NiFiProcessStatus(niFiInfo: NiFiInfo)
