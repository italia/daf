package it.gov.daf.securitymanager.service

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo, RestServiceResponse, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.sso.LoginClientLocal
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, Success}

import scala.concurrent.Future
import scala.util.{Failure, Try}

@Singleton
class WebHDFSApiClient @Inject()(secInvokeManager: SecuredInvocationManager, webHDFSApiProxy:WebHDFSApiProxy, implicit val cacheWrapper:CacheWrapper){

  import play.api.libs.concurrent.Execution.Implicits._

  private val HADOOP_URL = ConfigReader.hadoopUrl



  def setOwnershipForMigration(user:String,path:String):Future[Either[String,String]] = {

    Logger.logger.debug("setOwnershipForMigration: " + path)

    val loginInfo = readLoginInfo

    def fileList(): Future[Seq[String]] = {
      webHDFSApiProxy.callHdfsService("GET", path, Map("op" -> "LISTSTATUS"), Some(loginInfo)).map {
        case Right(r) =>  val lista = (r.jsValue \ "FileStatuses" \ "FileStatus" ).as[Seq[JsObject]]
                          if( lista.size>0 ) lista.map{elem => (elem \ "pathSuffix").as[String]}//((r.jsValue \ "FileStatuses" \ "FileStatus" \ "pathSuffix").as[Seq[String]])
                          else Seq.empty
        case Left(l) => throw new Exception(s"LISTSTATUS KO: $l")
      }
    }


    def setFilesOwnership(filenames: Seq[String]): Future[Seq[String]] = {

      def setOwner(filename: String): Future[String] = {

        webHDFSApiProxy.callHdfsService("PUT", s"$path/$filename", Map("op" -> "SETOWNER", "owner" -> user, "group" -> user), Some(loginInfo)).map {
          case Right(r) => s"$path/$filename OK "
          case Left(l) => throw new Exception(s"SETOWNER KO: $l")
        }

      }

      Logger.logger.debug("files: " + filenames)

      filenames.toList.traverse[Future, String](setOwner(_)): Future[Seq[String]]

    }


    val res = Try {
      for {
        fileList <- fileList()
        r2 <- setFilesOwnership(fileList)
      } yield r2
    }


    res match{
      case scala.util.Success(s) => s.map{ ss=> Right(ss.mkString("\n")) }
      case Failure(e) => Future.successful(Left(e.getMessage))
    }

  }



  def createHomeDir(userId:String):Future[Either[Error,Success]] = {

    Logger.logger.debug("createHomeDir: " + userId)

    val adminLoginInfo =  Some(new LoginInfo(ConfigReader.hdfsUser, ConfigReader.hdfsUserPwd, LoginClientLocal.HADOOP ))

    val res = for {
      r1 <- EitherT( webHDFSApiProxy.callHdfsService("PUT", s"uploads/$userId", Map("op" -> "MKDIRS", "permission" -> "700"),adminLoginInfo) )
      r2 <- EitherT( webHDFSApiProxy.callHdfsService("PUT", s"uploads/$userId", Map("op" -> "MODIFYACLENTRIES", "aclspec" -> s"user:$userId:rwx"),adminLoginInfo) )
      //op=SETACL&aclspec=user:impala:rwx,user:hive:rwx,user::rw-,group::r--,other::r--
      //r2 <- EitherT( webHDFSApiProxy.callHdfsService("PUT", s"uploads/$userId", Map("op" -> "SETOWNER", "owner" -> userId, "group" -> userId),adminLoginInfo) )
    }yield r2

    res.value map {
      case Right(r) => Right( Success(Some("Home dir created"), Some("ok")) )
      case Left(l) => Left( Error(Option(0), Some(l.jsValue.toString()), None) )
    }
  }

  def deleteHomeDir(userId:String):Future[Either[Error,Success]] = {

    Logger.logger.debug("deleteHomeDir: " + userId)

    val adminLoginInfo =  Some(new LoginInfo(ConfigReader.hdfsUser, ConfigReader.hdfsUserPwd, LoginClientLocal.HADOOP ))

    val res = webHDFSApiProxy.callHdfsService("DELETE", s"uploads/$userId", Map("op" -> "DELETE", "recursive"->"true"),adminLoginInfo)

    res map {
      case Right(r) => Right( Success(Some("Home dir deleted"), Some("ok")) )
      case Left(l) => Left( Error(Option(0), Some(l.jsValue.toString()), None) )
    }
  }


  private def handleServiceCall(serviceInvoke:(String,WSClient)=> Future[WSResponse], handleJson:(RestServiceResponse)=> Either[RestServiceResponse,JsValue] )={

    val loginInfo = readLoginInfo()

    secInvokeManager.manageRestServiceCallWithResp(loginInfo, serviceInvoke, 200,201,400,401,403,404).map {
      case Right(resp) => handleJson(resp)
      case Left(l) =>  Left(RestServiceResponse(JsString(l),500,None))
    }
  }


  def getAclStatus(path:String):Future[Either[RestServiceResponse,JsValue]] = {

    Logger.logger.debug("getAclStatus on path: " + path)


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(s"$HADOOP_URL/webhdfs/v1/$path?op=GETACLSTATUS").withHeaders("Cookie" -> cookie).get
    }

    def handleJson(resp:RestServiceResponse):Either[RestServiceResponse,JsValue]={

      (resp.jsValue \ "AclStatus").validate[JsObject] match {
        case JsSuccess(s,v) => Right(resp.jsValue)
        case JsError(e) => Left(resp)
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }


}



