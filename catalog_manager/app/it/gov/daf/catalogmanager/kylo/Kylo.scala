package it.gov.daf.catalogmanager.kylo

import catalog_manager.yaml.{InputSrcSrv_pullOpt, MetaCatalog, Success, Error}
import play.api.libs.json.{JsObject, JsResult, JsValue}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}
import com.google.inject.{Inject, Singleton}
import play.api.Logger

import scala.util.Try
//import org.apache.hadoop.hdfs.protocol.proto.HdfsProtos.HdfsFileStatusProto.FileType
import play.api.inject.ConfigurationProvider

import scala.concurrent.Future

@Singleton
class Kylo @Inject()(ws :WSClient, config: ConfigurationProvider){

  val KYLOURL = config.get.getString("kylo.url").get
  val KYLOUSER = config.get.getString("kylo.user").getOrElse("dladmin")
  val KYLOPWD = config.get.getString("kylo.pwd").getOrElse("XXXXXXXXXXX")

  import scala.concurrent.ExecutionContext.Implicits._

  private def getFeedInfo(feedName: String, user: String): Future[Either[Error, String]] ={
    val futureResponseFeedInfo = ws.url(KYLOURL + "/api/v1/feedmgr/feeds/by-name/" + feedName)
      .withAuth(KYLOUSER, KYLOPWD, WSAuthScheme.BASIC)
      .get()


    val res = futureResponseFeedInfo.map{
      resp =>
        val json = resp.json
        val id = (json \ "feedId").get.toString().replace("\"", "")
        val creator = (json \ "userProperties" \\ "value").map(v => v.toString().replace("\"", ""))
        if(creator.contains(user)) Right(id)
        else Left(Error("error in get info", Some(400), None))
    }

    res
  }

  def deleteFeed(feedName: String, user: String): Future[Either[Error, Success]] = {
    val responseDelete: Future[WSResponse] = for{
      idFeed <- getFeedInfo(feedName, user)
      if idFeed.isRight
      _ <- disableFeed(idFeed.right.get)
      resDelete <- delete(idFeed.right.get)
    } yield resDelete

    responseDelete onComplete(res =>
      Logger.logger.debug(s"response delete: ${res.isSuccess}")
      )

    responseDelete.map { res =>
      if (res.status == 204) Right(Success(s"$user delete $feedName", None))
      else Left(Error(res.statusText, Some(res.status), None))
    }
  }

  private def disableFeed(feedId: String): Future[Either[Error, Success]] = {
    val disbleFeedResponse: Future[WSResponse] = ws.url(KYLOURL + "/api/v1/feedmgr/feeds/disable/" + feedId)
      .withAuth(KYLOUSER, KYLOPWD, WSAuthScheme.BASIC)
      .post("")

    disbleFeedResponse.onComplete( r =>
      if(r.isSuccess)Logger.logger.debug(s"$feedId disabled")
      else Logger.logger.debug(s"$feedId not disabled")
    )

    disbleFeedResponse
      .map(res =>
        if(res.status == 200) Right(Success(s"$feedId disable", None))
        else Left(Error(s"$feedId not disable", Some(res.status), None))
      )
  }

  private def delete(feedId: String): Future[WSResponse] = {
    val futureResponseDelete = ws.url(KYLOURL + "/api/v1/feedmgr/feeds/" + feedId)
      .withAuth(KYLOUSER, KYLOPWD, WSAuthScheme.BASIC)
      .delete()
    futureResponseDelete.onComplete(r =>
      if(r.isSuccess) Logger.logger.debug(s"$feedId delete")
      else Logger.logger.debug(s"$feedId not delete")
    )
    futureResponseDelete
  }

  private def templateIdByName(templateName :String): Future[Option[String]] = {
    ws.url(KYLOURL + "/api/v1/feedmgr/templates/registered")
      .withAuth(KYLOUSER, KYLOPWD, scheme = WSAuthScheme.BASIC)
      .get()
      .map { resp =>
        val js: JsValue = resp.json
        val result = js.as[List[JsValue]]
          .filter(x => (x \ "templateName").as[String].equals(templateName))
        result match {
          case Nil => None
          case h :: _  => Some((h \ "id").as[String])
        }
      }
  }


  private def templatesById(id :Option[String],
                            fileType :String,
                            feed :MetaCatalog): Future[(JsValue, List[JsObject])] = {
    val idExt = id.getOrElse("")
    val url = s"/api/v1/feedmgr/templates/registered/$idExt?allProperties=true&feedEdit=true"
    ws.url(KYLOURL + url)
      .withAuth(KYLOUSER, KYLOPWD, scheme = WSAuthScheme.BASIC)
      .get().map { resp =>
      val templates = resp.json
      val templatesEditable = (templates \ "properties").as[List[JsValue]]
        .filter(x => { (x \ "userEditable").as[Boolean] })

      val template1 = templatesEditable(0).transform(KyloTrasformers.transformTemplates(KyloTrasformers.generateInputSftpPath(feed)))
      val template2 = templatesEditable(1).transform(KyloTrasformers.transformTemplates(".*" + fileType))
      //logger.debug(List(template1,template2).toString())
      (templates, List(template1.get, template2.get))
    }
  }

  private def webServiceTemplates(id :Option[String],
                                  fileType: String,
                                  feed :MetaCatalog): Future[(JsValue, List[JsObject])]
  = {
    val idExt = id.getOrElse("")
    val url = s"/api/v1/feedmgr/templates/registered/$idExt?allProperties=true&feedEdit=true"
    val testWs  = feed.operational.input_src.srv_pull.get.head.url
    val nameExp = feed.dcatapit.name + """_${now():format('yyyyMMddHHmmss')}"""
    ws.url(KYLOURL + url)
      .withAuth(KYLOUSER, KYLOPWD, scheme = WSAuthScheme.BASIC)
      .get().map { resp =>
      val templates = resp.json
      val templatesEditable  = (templates \ "properties").as[List[JsValue]]
        .filter(x => { (x \ "userEditable").as[Boolean] })
      val modifiedTemplates: List[JsObject] = templatesEditable.map { temp =>
        val key = (temp \ "key").as[String]
        val transTemplate = key match {
          case "URL" => temp.transform(KyloTrasformers.transformTemplates(testWs)).get
          case "Filename" => temp.transform(KyloTrasformers.transformTemplates(nameExp)).get
          case "Username" => temp.transform(KyloTrasformers.transformTemplates("dsda")).get
          case "Password" => temp.transform(KyloTrasformers.transformTemplates("dasds")).get
        }
        transTemplate
      }

      (templates, modifiedTemplates)
    }
  }

  private def sftpTemplates(id :Option[String],
                            fileType: String,
                            feed :MetaCatalog): Future[(JsValue, List[JsObject])]
  = {
    val idExt = id.getOrElse("")
    val url = s"/api/v1/feedmgr/templates/registered/$idExt?allProperties=true&feedEdit=true"
    val sftpRemote  = feed.operational.input_src.sftp.get.head
    ws.url(KYLOURL + url)
      .withAuth(KYLOUSER, KYLOPWD, scheme = WSAuthScheme.BASIC)
      .get().map { resp =>
      val templates = resp.json
      val templatesEditable  = (templates \ "properties").as[List[JsValue]]
        .filter(x => { (x \ "userEditable").as[Boolean] })
      val modifiedTemplates: List[JsObject] = templatesEditable.map { temp =>
        val key = (temp \ "key").as[String]
        val transTemplate = key match {
          case "Hostname" => temp.transform(KyloTrasformers.transformTemplates(sftpRemote.url.get)).get
          case "Username" => temp.transform(KyloTrasformers.transformTemplates(sftpRemote.username.get)).get
          case "Password" => temp.transform(KyloTrasformers.transformTemplates("{cypher}" + sftpRemote.password.get)).get
          case "Remote Path" => temp.transform(KyloTrasformers.transformTemplates(sftpRemote.param.get)).get
        }
        transTemplate
      }

      (templates, modifiedTemplates)
    }
  }


  def datasetIngest(fileType :String, meta :MetaCatalog) :Future[(JsValue, List[JsObject])] = {
    for {
      idOpt <- templateIdByName("Dataset Ingest")
      templates <- templatesById(idOpt, fileType, meta)
    } yield templates
  }

  def wsIngest(fileType: String, meta: MetaCatalog): Future[(JsValue, List[JsObject])] = {
    for {
      idOpt <- templateIdByName("Webservice Ingest")
      templates <- webServiceTemplates(idOpt, fileType, meta)
    } yield templates
  }

  def sftpRemoteIngest(fileType: String, meta: MetaCatalog): Future[(JsValue, List[JsObject])] = {
    for {
      idOpt <- templateIdByName("Sftp Ingest")
      templates <- sftpTemplates(idOpt, fileType, meta)
    } yield templates
  }


  def categoryFuture(meta :MetaCatalog): Future[JsValue] = {
    val categoriesWs = ws.url(KYLOURL + "/api/v1/feedmgr/categories")
      .withAuth(KYLOUSER, KYLOPWD, scheme = WSAuthScheme.BASIC)
      .get()

    val categoryFuture = categoriesWs.map { x =>
      val categoriesJson = x.json
      val categories = categoriesJson.as[List[JsValue]]
      val found =categories.filter(cat => {(cat \ "systemName").as[String].equals(meta.dcatapit.owner_org.get)})
      found.head
    }
    categoryFuture
  }

}
