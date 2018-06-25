package it.gov.daf.securitymanager.service

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo, RestServiceResponse, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.{ConfigReader, RequestContext}
import it.gov.daf.sso.LoginClientLocal
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, Success}

import scala.concurrent.Future

@Singleton
class WebHDFSApiClient @Inject()(secInvokeManager: SecuredInvocationManager, webHDFSApiProxy:WebHDFSApiProxy, implicit val cacheWrapper:CacheWrapper){

  import play.api.libs.concurrent.Execution.Implicits._

  private val HADOOP_URL = ConfigReader.hadoopUrl

  /*
  private def getLoginInfo()={

    val userName = RequestContext.getUsername()
    val pwd = cacheWrapper.getPwd(userName) match {
      case Some(x) =>x
      case None => throw new Exception("User passoword not in cache")
    }

    new LoginInfo( RequestContext.getUsername(), pwd, LoginClientLocal.HADOOP )
  }*/

  def createHomeDir(userId:String):Future[Either[Error,Success]] = {

    Logger.logger.debug("createHomeDir: " + userId)

    val adminLoginInfo =  Some(new LoginInfo(ConfigReader.impalaAdminUser, ConfigReader.impalaAdminUserPwd, LoginClientLocal.HADOOP ))

    val res = for {
      r1 <- EitherT( webHDFSApiProxy.callHdfsService("PUT", s"user/$userId", Map("op" -> "MKDIRS"),adminLoginInfo) )
      r2 <- EitherT( webHDFSApiProxy.callHdfsService("PUT", s"user/$userId", Map("op" -> "SETOWNER", "owner" -> userId, "group" -> userId),adminLoginInfo) )
    }yield r2

    res.value map {
      case Right(r) => Right( Success(Some("Home dir created"), Some("ok")) )
      case Left(l) => Left( Error(Option(0), Some(l.jsValue.toString()), None) )
    }
  }

  def deleteHomeDir(userId:String):Future[Either[Error,Success]] = {

    Logger.logger.debug("deleteHomeDir: " + userId)

    val adminLoginInfo =  Some(new LoginInfo(ConfigReader.impalaAdminUser, ConfigReader.impalaAdminUserPwd, LoginClientLocal.HADOOP ))

    val res = webHDFSApiProxy.callHdfsService("DELETE", s"user/$userId", Map("op" -> "DELETE", "recursive"->"true"),adminLoginInfo)

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



