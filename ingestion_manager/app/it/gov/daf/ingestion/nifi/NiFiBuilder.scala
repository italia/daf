package it.gov.daf.ingestion.nifi

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import it.gov.daf.catalogmanager._
import it.gov.daf.ingestion.metacatalog.MetaCatalogProcessor
import play.api.libs.json.{JsValue, _}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

//class NiFiBuilder(metaCatalogProcessor: MetaCatalogProcessor) {
@SuppressWarnings(
  Array(
    "org.wartremover.warts.ToString"
  )
) //FIXME when is @Inject used?
class NiFiBuilder @Inject() (ws: WSClient) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val nifiUrl: String = ConfigFactory.load().getString("WebServices.nifiUrl")
  val nifiFunnelId: String = ConfigFactory.load().getString("ingmgr.niFi.funnelId")
  val nifiGroupId: String = ConfigFactory.load().getString("ingmgr.niFi.groupId")



  private def createConnection(src: Any, dst: Any) = {

  }

  private def startProcessor(processors: List[Any]) = {

  }

  def createProcessors(metaCatalog: MetaCatalog): NiFiProcessStatus = {
    val metaCatalogProc = new MetaCatalogProcessor(metaCatalog)

    val inputSrc: InputSrc = metaCatalogProc.inputSrc()

    val sftpDefinitions: immutable.Seq[Try[(JsValue, Map[String, String])]] = inputSrc.sftp
      .getOrElse(List.empty)
      .map(x => defineSftpProcessor(metaCatalogProc, x))


//    var srvPullCount: Int = 0
//    val inputSrvPullList: List[(String, Boolean)] = inputSrc.srv_pull match {
//      case Some(s) =>
//        println("procListeners - SrvPull: " + s.toString)
//        srvPullCount += 1
//        s.map(x => (x.toString -> activateFlow(procListenerSrvPull(x, srvPullCount.toString))))
//      case None => List()
//    }
//
//    var srvPushCount: Int = 0
//    val inputSrvPushList: List[(String, Boolean)] = inputSrc.srv_push match {
//      case Some(s) =>
//        println("procListeners - SrvPush: " + s.toString)
//        srvPushCount += 1
//        s.map(x => (x.toString -> activateFlow(procListenerSrvPush(x, srvPushCount.toString))))
//      case None => List()
//    }
//
//    var dafDsCount: Int = 0
//    val inputDafDsList: List[(String, Boolean)] = inputSrc.daf_dataset match {
//      case Some(s) =>
//        println("procListeners - DafDs: " + s.toString)
//        dafDsCount += 1
//        //s.map(x => procListenerDafDs())
//        List()
//      case None => List()
//    }


    //    val outputList: List[(String, Boolean)] = (inputSftpList ++ inputSrvPullList ++ inputSrvPushList).toList
    //    val status = if (outputList.map(x => x._2).reduceLeft((x, y) => x || y)) "OK" else "ERRORS"
    //    NiFiProcessStatus(status, NiFiInfo(metaCatalogProc.dsName, outputList))

    //create processors
    //link processors
    //connect to the funnel
    //play the processors

    ???
  }

  def activateFlow(procListInfo: (Future[WSResponse], Map[String, String])): Boolean = {
    val (listenerFuture: Future[WSResponse], params: Map[String, String]) = (procListInfo._1, procListInfo._2)
    var idListener: String = ""
    var idUpdateAttr: String = ""

    val futureProcSetup: Future[WSResponse] = for {
      resListener <- listenerFuture

      resUpdateAttr <- {
        println("ProcListener -> " + resListener.toString)
        idListener = (resListener.json \ "id").as[String]
        processorUpdateAttr(params)
      }

      resConnListAttr <- {
        println("ProcUpdateAttr -> " + resUpdateAttr.toString)
        idUpdateAttr = (resUpdateAttr.json \ "id").as[String]
        connListAttr((resListener.json \ "id").as[String], (resUpdateAttr.json \ "id").as[String])
      }

      //Thi may need to change based on the type of flux managed
      resConnFunnel <- {
        println("ConnListAttr -> " + resConnListAttr.toString)
        connFunnel((resUpdateAttr.json \ "id").as[String], nifiFunnelId)
      }

      resPlayListener <- {
        println("resConnFunnel -> " + resConnFunnel.toString)
        playProc(
          (resListener.json \ "id").as[String],
          "RUNNING",
          (resListener.json \ "revision" \ "clientId").as[String],
          (resListener.json \ "revision" \ "version").as[Int].toString
        )
      }

      resPlayUpdateAttr <- {
        println("resPlayListener -> " + resPlayListener.toString)
        playProc(
          (resUpdateAttr.json \ "id").as[String],
          "RUNNING",
          (resUpdateAttr.json \ "revision" \ "clientId").as[String],
          (resUpdateAttr.json \ "revision" \ "version").as[Int].toString
        )
      }

    } yield (resPlayUpdateAttr)

    val result: Try[WSResponse] = Await.ready(futureProcSetup, Duration.Inf).value.get

    result match {
      case Success(s) =>
        println("resPlayUpdateAttr -> " + s.toString)
        println("NiFi Flow sucessfully created!")
        true

      case Failure(e) =>
        println(e.printStackTrace())
        println("Destroying all created but pending processes...")
        //TODO here need to put code to destroy what has been created above. (stop them first)
        false
    }

  }

  private[this] var counter = 0
  private def incCounter() = {
    counter += 1
    counter
  }

  /**
    *
    * @param metaCatalogProc
    * @param src
    * @return a JValue and Map[String,String] containing the Json definition and the parameters for a processor
    */
  private def defineSftpProcessor(metaCatalogProc: MetaCatalogProcessor, src: SourceSftp): Try[(JsValue, Map[String, String])] = {
    val uniqueVal = incCounter()
    val name = metaCatalogProc.dsName + "_sftp_" + uniqueVal

    src match {
      case SourceSftp("sftp_daf", None, None, None, _) =>
        //internal DAF path
        val processor = NifiHelper.listFileProcessor(
          name = name,
          inputDir = metaCatalogProc.sourceSftpPathDefault()
        )

        val params: Map[String, String] = Map(
          "inputType" -> "local",
          "inputDir" -> metaCatalogProc.sourceSftpPathDefault(),
          "inputDirLocation" -> "Local",
          "clientId" -> name,
          "name" -> name,
          "user" -> "",
          "pass" -> "",
          "uniqueVal" -> uniqueVal.toString
        )
        Success(processor -> params)

      case SourceSftp(ext, Some(remoteUrl), Some(user), Some(pwd), _) =>
        //external sftp
        //FIXME
        val inputPathLocation = "?"

        val processor = NifiHelper.listSftpProcessor(
          name = name,
          hostname = inputPathLocation,
          user = user,
          pass = pwd
        )

        val params: Map[String, String] = Map(
          "inputType" -> "sftp",
          "inputDir" -> metaCatalogProc.sourceSftpPathDefault(),
          "inputDirLocation" -> inputPathLocation,
          "clientId" -> name,
          "name" -> name,
          "user" -> user,
          "pass" -> pwd,
          "uniqueVal" -> uniqueVal.toString
        )
        Success(processor -> params)

      case other =>
      //error
        Failure(new IllegalArgumentException(s"invalid value $other"))
    }
  }

  private def createProcessor(json: JsValue, attributes: Map[String, String]): Future[(JsValue, Map[String, String], WSResponse)] = {
    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
    logger.debug(s"posting processor $json to ${request.url}")
    request.post(json)
      .map(response => (json, attributes, response))
  }


  def procListenerSrvPull(srvInfo: SourceSrvPull, uniqueVal: String): (Future[WSResponse], Map[String, String]) = {

//    val (inputDir: String, user: String, pass: String, token: String) = srvInfo match {
//      case SourceSrvPull(name, url, Some(user), Some(pass), Some(token), _) => (url, user, pass, token)
//      case SourceSrvPull(name, url, Some(user), Some(pass), None, _) => (url, user, pass, "")
//      case SourceSrvPull(name, url, None, None, token, _) => (url, "", "", token)
//      case _ =>
//        logger.warn("procListenerSrvPull: something went wrong with Srv configuration \t SourceSrvPull: " + srvInfo.toString)
//        ("error", "error", "error", "error")
//    }
//
//    //1. Create the Listener Processor
//
//    //TODO Change here for the correct processor for this type
//    val json = NifiHelper.listFileProcessor(
//      clientId = metaCatalogProc.dsName + "_srvPull_" + uniqueVal,
//      name = metaCatalogProc.dsName + "_srvPull_" + uniqueVal,
//      inputDir = "test",
//      inputDirLocation = "test"
//    )
//
//    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    println(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    val futureResponse: Future[WSResponse] = request.post(json)
//
//    //2. prepare the parameters to be sent to UpdateAttr Processor
//    //TODO Change here for the correct processor for this type
//    val params: Map[String, String] = Map(
//      "inputDir" -> inputDir,
//      "clientId" -> (metaCatalogProc.dsName + "_srvPullUpdate_" + uniqueVal),
//      "name" -> (metaCatalogProc.dsName + "_srvPullUpdate_" + uniqueVal),
//      "user" -> user,
//      "pass" -> pass,
//      "uniqueVal" -> uniqueVal
//    )
//
//    (futureResponse, params)
???
  }

  def procListenerSrvPush(srvInfo: SourceSrvPush, uniqueVal: String): (Future[WSResponse], Map[String, String]) = {

    val (inputDir: String, user: String, pass: String, token: String) = srvInfo match {
      case SourceSrvPush(name, url, Some(user), Some(pass), Some(token), _) => (url, user, pass, token)
      case SourceSrvPush(name, url, Some(user), Some(pass), None, _) => (url, user, pass, "")
      case SourceSrvPush(name, url, None, None, token, _) => (url, "", "", token)
      case _ =>
        logger.warn("procListenerSrvPush: something went wrong with Srv configuration \t SourceSrvPush: " + srvInfo.toString)
        ("error", "error", "error", "error")
    }

    //1. Create the Listener Processor
//    val (inputDir: String, user: String, pass: String, token: String) = srvInfo match {
//      case SourceSrvPull(name, url, Some(user), Some(pass), Some(token), _) => (url, user, pass, token)
//      case SourceSrvPull(name, url, Some(user), Some(pass), None, _) => (url, user, pass, "")
//      case SourceSrvPull(name, url, None, None, token, _) => (url, "", "", token)
//      case _ =>
//        logger.warn("procListenerSrvPull: something went wrong with Srv configuration \t SourceSrvPull: " + srvInfo.toString)
//        ("error", "error", "error", "error")
//    }
//
//    //1. Create the Listener Processor
//
//    //TODO Change here for the correct processor for this type
//    val json = NifiHelper.listFileProcessor(
//      clientId = metaCatalogProc.dsName + "_srvPull_" + uniqueVal,
//      name = metaCatalogProc.dsName + "_srvPull_" + uniqueVal,
//      inputDir = "test",
//      inputDirLocation = "test"
//    )
//
//    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    println(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    val futureResponse: Future[WSResponse] = request.post(json)
//
//    //2. prepare the parameters to be sent to UpdateAttr Processor
//    //TODO Change here for the correct processor for this type
//    val params: Map[String, String] = Map(
//      "inputDir" -> inputDir,
//      "clientId" -> (metaCatalogProc.dsName + "_srvPullUpdate_" + uniqueVal),
//      "name" -> (metaCatalogProc.dsName + "_srvPullUpdate_" + uniqueVal),
//      "user" -> user,
//      "pass" -> pass,
//      "uniqueVal" -> uniqueVal
//    )
//
//    (futureResponse, params)

//    //TODO Change here for the correct processor for this type
//    val json = NifiHelper.listFileProcessor(
//      clientId = metaCatalogProc.dsName + "_srvPush_" + uniqueVal,
//      name = metaCatalogProc.dsName + "_srvPush_" + uniqueVal,
//      inputDir = "test",
//      inputDirLocation = "test"
//    )
//
//    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    println(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    val futureResponse: Future[WSResponse] = request.post(json)
//
//    //2. prepare the parameters to be sent to UpdateAttr Processor
//    //TODO Change here for the correct processor for this type
//    val params: Map[String, String] = Map(
//      "inputDir" -> inputDir,
//      "clientId" -> (metaCatalogProc.dsName + "_srvPushUpdate_" + uniqueVal),
//      "name" -> (metaCatalogProc.dsName + "_srvPushUpdate_" + uniqueVal),
//      "user" -> user,
//      "pass" -> pass,
//      "uniqueVal" -> uniqueVal
//    )
//
//    (futureResponse, params)

    ???
  }

  //TODO - to be implemented
  def procListenerDafDs(): List[Future[WSResponse]] = {
    List()
  }

  def processorUpdateAttr(param: Map[String, String]): Future[WSResponse] = {
//    val uniqueVal = param.getOrElse("uniqueVal", "")
//    val json: JsValue = NifiHelper.updateAttrProcessor(
//      clientId = metaCatalogProc.dsName + "_updateAttr_" + uniqueVal,
//      name = metaCatalogProc.dsName + "_updateAttr_" + uniqueVal,
//      inputSrc = metaCatalogProc.inputSrcNifi(),
//      storage = metaCatalogProc.storageNifi(),
//      dataschema = metaCatalogProc.dataschemaNifi(),
//      dataset_type = metaCatalogProc.dataset_typeNifi(),
//      transfPipeline = metaCatalogProc.ingPipelineNifi(),
//      format = metaCatalogProc.fileFormatNifi()
//    )
//
//    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    println("processorUpdateAttr - url: " + nifiUrl + "process-groups/" + nifiGroupId + "/processors")
//    val futureResponse: Future[WSResponse] = request.post(json)
//
//    futureResponse
    ???
  }

  /**
    * create the connection between two processors
    * @param idListener
    * @param idUpdateAttr
    * @param uniqueVal
    * @return
    */
  def connListAttr(idListener: String, idUpdateAttr: String, uniqueVal: String = ""): Future[WSResponse] = {
//    val json: JsValue = NifiHelper.defineConnection(
//      clientId = metaCatalogProc.dsName + "_connListAttr_" + uniqueVal,
//      name = metaCatalogProc.dsName + "_connListAttr_" + uniqueVal,
//      sourceId = idListener,
//      sourceGroupId = nifiGroupId,
//      sourceType = "PROCESSOR",
//      destId = idUpdateAttr,
//      destGroupId = nifiGroupId,
//      destType = "PROCESSOR"
//    )
//
//    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/connections")
//    println("connListAttr - url: " + nifiUrl + "process-groups/" + nifiGroupId + "/connections")
//    val futureResponse: Future[WSResponse] = request.post(json)
//
//    futureResponse
    ???
  }

  /**
    * connect a processor to the funnel
    * @param idUpdateAttr
    * @param idFunnel
    * @return
    */
  def connFunnel(idUpdateAttr: String, idFunnel: String): Future[WSResponse] = {
//    val json: JsValue = NifiHelper.defineConnection(
//      clientId = metaCatalogProc.dsName + "_updateAttr",
//      name = metaCatalogProc.dsName + "_updateAttr",
//      sourceId = idUpdateAttr,
//      sourceGroupId = nifiGroupId,
//      sourceType = "PROCESSOR",
//      destId = idFunnel,
//      destGroupId = nifiGroupId,
//      destType = "PROCESSOR"
//    )
//
//    val request: WSRequest = ws.url(nifiUrl + "process-groups/" + nifiGroupId + "/connections")
//    println("connFunnel - url: " + nifiUrl + "process-groups/" + nifiGroupId + "/connections")
//    val futureResponse: Future[WSResponse] = request.post(json)
//
//    futureResponse
    ???
  }

  def playProc(
    componentId: String,
    componentState: String,
    clientId: String,
    version: String
  ): Future[WSResponse] = {

    val json: JsValue = NifiHelper.playProcessor(
      clientId = clientId + "_play",
      componentId = componentId,
      componentState = componentState,
      version = version
    )

    println("playProc - Url: " + nifiUrl + "processors/" + componentId)
    println("playProc - json: " + json.toString)

    val request: WSRequest = ws.url(nifiUrl + "processors/" + componentId)

    val futureResponse: Future[WSResponse] = request.put(json)

    futureResponse
  }

  //  PUT http://edge1:9090/nifi-api/processors/6f7b46a2-aa15-139f-3083-addf34976b6e

}

final case class NiFiInfo(dsName: String, procList: List[(String, Boolean)])
final case class NiFiProcessStatus(status: String, niFiInfo: NiFiInfo)

/*
**************
* backup
*
  def processorBuilder(): NiFiProcessStatus = {
    //val niFiInfo: NiFiInfo = getNiFiInfo()
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
    val json: JsValue = NifiJson.listFileProc(
      clientId = "test1",
      name = "test1",
      inputDir = "/home/davide/test"
    )

    val request: WSRequest = ws.url(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    println(nifiUrl+"process-groups/6f7b46a2-aa15-139f-3083-addf34976b6e/processors")
    val futureResponse: Future[WSResponse] = request.post(json)

    futureResponse
  }

 */

