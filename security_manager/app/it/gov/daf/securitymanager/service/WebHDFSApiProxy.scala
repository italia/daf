package it.gov.daf.securitymanager.service

import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{CacheWrapper, RestServiceResponse, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.{ConfigReader}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import scala.concurrent.Future


@Singleton
class WebHDFSApiProxy @Inject()(secInvokeManager: SecuredInvocationManager,implicit val cacheWrapper:CacheWrapper){

  import play.api.libs.concurrent.Execution.Implicits._

  private val logger = Logger(this.getClass.getName)
  private val HADOOP_URL = ConfigReader.hadoopUrl


  private def handleServiceCall(serviceInvoke:(String,WSClient)=> Future[WSResponse] )={

    val loginInfo = readLoginInfo()

    secInvokeManager.manageRestServiceCallWithResp(loginInfo, serviceInvoke, 200,201,307,400,401,403,404).map {
      case Right(resp) => if(resp.httpCode<400) Right(resp)
                          else Left(resp)
      case Left(l) =>  Left(RestServiceResponse(JsString(l),500,None))
    }
  }


  def callHdfsService( httpMethod: String, path:String, params:Map[String,String] ):Future[Either[RestServiceResponse,RestServiceResponse]] = {

    Logger.logger.debug(s"callService on path:$path with method:$httpMethod and params: $params")


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      val prmList = params.toList
      logger.debug(s"---->$prmList")
      val response = wsClient.url(s"$HADOOP_URL/webhdfs/v1/$path").withHeaders("Cookie" -> cookie).withFollowRedirects(false).withMethod(httpMethod).withQueryString(prmList:_*).execute
      logger.debug(s"callHdfsService Response: $response")
      response
    }

    handleServiceCall(serviceInvoke)

  }


}



