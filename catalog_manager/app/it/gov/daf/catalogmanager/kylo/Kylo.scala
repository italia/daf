package it.gov.daf.catalogmanager.kylo

import catalog_manager.yaml.{InputSrcSrv_pullOpt, MetaCatalog, SourceSftp}
import play.api.libs.json.{JsObject, JsResult, JsValue}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}
import com.google.inject.{Inject, Singleton}
//import org.apache.hadoop.hdfs.protocol.proto.HdfsProtos.HdfsFileStatusProto.FileType
import play.api.inject.ConfigurationProvider
import play.api.Logger

import scala.concurrent.Future

@Singleton
class Kylo @Inject()(ws :WSClient, config: ConfigurationProvider){

  val KYLOURL = config.get.getString("kylo.url").get
  val KYLOUSER = config.get.getString("kylo.user").getOrElse("dladmin")
  val KYLOPWD = config.get.getString("kylo.userpwd").getOrElse("XXXXXXXXXXX")

  import scala.concurrent.ExecutionContext.Implicits._

  private def templateIdByName(templateName :String): Future[Option[String]] = {
    ws.url(KYLOURL + "/api/v1/feedmgr/templates/registered")
      .withAuth(KYLOUSER, KYLOPWD, scheme = WSAuthScheme.BASIC)
      .get()
      .map { resp =>
        val js: JsValue = resp.json
        val result = js.as[List[JsValue]]
          .filter(x => (x \ "templateName").as[String].equals(templateName))
        val res = result match {
          case Nil => None
          case h :: _  => Some((h \ "id").as[String])
        }
        Logger.logger.debug(s"templateId: $res")
        res
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

  private def hdfsTemplate(id: Option[String], fileType: String, hdfsPath: String) = {
    val url = s"/api/v1/feedmgr/templates/registered/${id.getOrElse("")}?allProperties=true&feedEdit=true"
    ws.url(KYLOURL + url)
      .withAuth(KYLOUSER, KYLOPWD, WSAuthScheme.BASIC)
      .get().map { resp =>
      val template = resp.json
      val propertiesEditable = (template \ "properties").as[List[JsValue]]
        .filter(x => (x \ "userEditable").as[Boolean])
      val modifiedTemplate: List[JsObject] = propertiesEditable.map { temp =>
        val key = (temp \ "key").as[String]
        val transTemplate = key match {
          case "File Filter Regex" => temp.transform(KyloTrasformers.transformTemplates(".*" + fileType)).get
          case "Directory" => temp.transform(KyloTrasformers.transformTemplates(hdfsPath)).get
        }
        transTemplate
      }

      (template, modifiedTemplate)
    }
  }

  private def sftpTemplates(id :Option[String],
                            fileType: String,
                            sftpPath: String): Future[(JsValue, List[JsObject])]
  = {
    val idExt = id.getOrElse("")
    val url = s"/api/v1/feedmgr/templates/registered/$idExt?allProperties=true&feedEdit=true"
    ws.url(KYLOURL + url)
      .withAuth(KYLOUSER, KYLOPWD, scheme = WSAuthScheme.BASIC)
      .get().map { resp =>
      val templates = resp.json
      val templatesEditable  = (templates \ "properties").as[List[JsValue]]
        .filter(x => { (x \ "userEditable").as[Boolean] })
      val modifiedTemplates: List[JsObject] = templatesEditable.map { temp =>
        val key = (temp \ "key").as[String]
        val transTemplate = key match {
          case "Hostname" => temp.transform(KyloTrasformers.transformTemplates("sftp.teamdigitale.test")).get
          case "Remote Path" => temp.transform(KyloTrasformers.transformTemplates(sftpPath)).get
          case "File Filter Regex" => temp.transform(KyloTrasformers.transformTemplates(".*" + fileType)).get
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

  def sftpRemoteIngest(fileType: String, sftpPath: String): Future[(JsValue, List[JsObject])] = {
    for {
      idOpt <- templateIdByName("Sftp Ingest")
      templates <- sftpTemplates(idOpt, fileType, sftpPath)
    } yield templates
  }

  def hdfsIngest(fileType: String, hdfsPath: String) = {
    for{
      idOpt <- templateIdByName("HDFS Ingest")
      templates <- hdfsTemplate(idOpt, fileType, hdfsPath)
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
