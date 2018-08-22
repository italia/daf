package controllers

import javax.inject.Inject

import it.gov.daf.securitymanager.service.{ImpalaService, WebHDFSApiClient}
import it.gov.daf.sso.{ApiClientIPA, RoleGroup}
import play.api.mvc.{Action, Controller, Result}
import it.gov.daf.securitymanager.service.utilities.RequestContext.execInContext

import scala.concurrent.Future


// These services are only for migration or test pourposes
class MigrationController  @Inject()(apiClientIPA: ApiClientIPA,impalaService: ImpalaService, webHDFSApiClient: WebHDFSApiClient) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  def createRole(roleName:String) = Action.async { implicit request =>

      apiClientIPA.createGroup(RoleGroup(roleName),None).map{
        case Right(s) => Ok(s"Role: $roleName created")
        case Left(l) => InternalServerError(l.toString)
      }

  }


  def createImpalaRole = Action { implicit request =>

    val json = request.body.asJson.get
    val name = (json \ "roleName").as[String]
    val isUser = (json \ "isUser").as[Boolean]

    impalaService.createRole(name,isUser) match{
      case Right(r) => Ok(r.toString)
      case Left(l) => InternalServerError(l.toString)

    }

  }


  def createHDFShomeDir(userId:String) = Action.async { implicit request =>

    webHDFSApiClient.createHomeDir(userId).map{
      case Right(s) => Ok(s"directoy /uploads/$userId created")
      case Left(l) => InternalServerError(l.toString)
    }

  }



}
