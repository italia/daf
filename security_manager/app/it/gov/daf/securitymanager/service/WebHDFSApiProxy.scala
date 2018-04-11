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
class WebHDFSApiProxy @Inject()(secInvokeManager: SecuredInvocationManager, cacheWrapper:CacheWrapper){

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


  private def handleServiceCall(serviceInvoke:(String,WSClient)=> Future[WSResponse] )={

    val loginInfo = getLoginInfo()

    secInvokeManager.manageRestServiceCallWithResp(loginInfo, serviceInvoke, 200,201,400,401,403,404).map {
      case Right(resp) => if(resp.httpCode<400) Right(resp)
                          else Left(resp)
      case Left(l) =>  Left(RestServiceResponse(JsString(l),500))
    }
  }


  def callHdfsService( httpMethod: String, path:String, params:Map[String,String] ):Future[Either[RestServiceResponse,RestServiceResponse]] = {

    Logger.logger.debug("callService on path: " + path)


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      wsClient.url(s"$HADOOP_URL/webhdfs/v1/$path").withHeaders("Cookie" -> cookie).withMethod(httpMethod).withQueryString(params.toList:_*).execute
    }

    handleServiceCall(serviceInvoke)

  }


}



