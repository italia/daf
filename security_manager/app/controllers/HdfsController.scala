package controllers

import javax.inject.Inject

import it.gov.daf.securitymanager.service.{WebHDFSApiClient, WebHDFSApiProxy}
import it.gov.daf.securitymanager.service.utilities.RequestContext.execInContext
import play.api.libs.ws.WSClient
import play.api.mvc._
import scala.concurrent.Future

class HdfsController @Inject()(ws: WSClient, webHDFSApiClient:WebHDFSApiClient, webHDFSApiProxy:WebHDFSApiProxy) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  //----------------SECURED API---------------------------------------

  def retriveACL(path:String) = Action.async { implicit request =>
    execInContext[Future[Result]]("retriveACL") { () =>

      webHDFSApiClient.getAclStatus(path).map {
        case Right(r) => Ok(r)
        //case Left(l) => new Status(l.httpCode).apply(l.jsValue.toString())//Status(l.httpCode).apply(l.jsValue)
        case Left(l) => new Status(l.httpCode).apply(l.jsValue.toString())
      }

    }
  }

  def callWebHdfs(path:String) = Action.async { implicit request =>
    execInContext[Future[Result]]("callWebHdfs") { () =>

      val queryString: Map[String, String] = request.queryString.map { case (k,v) => k -> v.mkString}

      webHDFSApiProxy.callHdfsService(request.method, path, queryString).map {
        case Right(r) => Ok(r.jsValue)
        case Left(l) => new Status(l.httpCode).apply(l.jsValue.toString())
      }

    }
  }


}