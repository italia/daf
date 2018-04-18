package it.gov.daf.securitymanager.service

import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo, RestServiceResponse, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.{ConfigReader, RequestContext}
import it.gov.daf.sso.LoginClientLocal
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future

@Singleton
class WebHDFSApiClient @Inject()(secInvokeManager: SecuredInvocationManager, cacheWrapper:CacheWrapper){

  import play.api.libs.concurrent.Execution.Implicits._

  private val HADOOP_URL = ConfigReader.hadoopUrl

  private def getLoginInfo()={

    val userName = RequestContext.getUsername()
    val pwd = cacheWrapper.getPwd(userName) match {
      case Some(x) =>x
      case None => throw new Exception("User passoword not in cache")
    }

    new LoginInfo( RequestContext.getUsername(), pwd, LoginClientLocal.HADOOP )
  }


  private def handleServiceCall(serviceInvoke:(String,WSClient)=> Future[WSResponse], handleJson:(RestServiceResponse)=> Either[RestServiceResponse,JsValue] )={

    val loginInfo = getLoginInfo()

    secInvokeManager.manageRestServiceCallWithResp(loginInfo, serviceInvoke, 200,201,400,401,403,404).map {
      case Right(resp) => handleJson(resp)
      case Left(l) =>  Left(RestServiceResponse(JsString(l),500))
    }
  }


  def getAclStatus(path:String):Future[Either[RestServiceResponse,JsValue]] = {

    Logger.logger.debug("getAclStatus on path: " + path)


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(s"$HADOOP_URL/webhdfs/v1/$path?op=GETACLSTATUS").withHeaders("Cookie" -> cookie).get
    }

    def handleJson(resp:RestServiceResponse):Either[RestServiceResponse,JsValue]={

      (resp.jsValue \ "AclStatus").validate[JsObject] match {
        case s: JsSuccess[String] => Right(resp.jsValue)
        case e: JsError => Left(resp)
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }


}



