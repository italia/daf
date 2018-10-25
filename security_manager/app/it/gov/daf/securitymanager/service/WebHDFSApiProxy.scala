package it.gov.daf.securitymanager.service

import java.time.Instant

import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo, RestServiceResponse, SecuredInvocationManager}
import it.gov.daf.securitymanager.utilities.ConfigReader
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import java.util.Date

import scala.concurrent.Future


@Singleton
class WebHDFSApiProxy @Inject()(secInvokeManager: SecuredInvocationManager, implicit val cacheWrapper:CacheWrapper){

  import play.api.libs.concurrent.Execution.Implicits._
  import scala.language.implicitConversions

  private val logger = Logger(this.getClass.getName)
  private val HADOOP_URLS = Array(ConfigReader.hadoopUrl,ConfigReader.hadoopUrl2)

  private var hadoopUrlIdx = false
  private var endpointSwitchDate = Date.from(Instant.EPOCH)

  implicit def bool2int(b:Boolean):Int= if (b) 1 else 0

  def switchEndpoint():Unit = synchronized {

    if( HADOOP_URLS(1).length > 0 && ((new Date).getTime-endpointSwitchDate.getTime) > 5000 ) {
      endpointSwitchDate = new Date
      hadoopUrlIdx = !hadoopUrlIdx

      logger.info(s"Switching hadoop url to ${HADOOP_URLS(hadoopUrlIdx)}")
    }

  }


  private def handleServiceCall( serviceInvoke:(String,WSClient)=> Future[WSResponse], loginInfoParam:Option[LoginInfo] )={


    val loginInfo = loginInfoParam.getOrElse(readLoginInfo)

    secInvokeManager.manageRestServiceCallWithResp(loginInfo, serviceInvoke, 200,201,307,400,401,403,404).map {
      case Right(resp) => if(resp.httpCode<400) Right(resp)
                          else Left(resp)
      case Left(l) =>  Left(RestServiceResponse(JsString(l),500,None))
    }
  }


  def callHdfsService( httpMethod: String, path:String, params:Map[String,String], loginInfoParam:Option[LoginInfo] ):Future[Either[RestServiceResponse,RestServiceResponse]] = {

    Logger.logger.debug(s"callService on path:$path with method:$httpMethod and params: $params")


    def serviceInvoke(cookie: String, wsClient: WSClient): Future[WSResponse] = {
      val prmList = params.toList
      logger.debug(s"---->$prmList")
      val response = wsClient.url(s"${HADOOP_URLS(hadoopUrlIdx)}/webhdfs/v1/$path").withHeaders("Cookie" -> cookie).withFollowRedirects(false).withMethod(httpMethod).withQueryString(prmList:_*).execute
      logger.debug(s"callHdfsService Response: $response")
      response
    }

    handleServiceCall(serviceInvoke,loginInfoParam)

  }


}



