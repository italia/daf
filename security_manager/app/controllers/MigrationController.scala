package controllers

import javax.inject.Inject

import cats.data.EitherT
import cats.implicits._
import it.gov.daf
import it.gov.daf.common.sso.common.{Editor, Role, Viewer}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.securitymanager.service.{ImpalaService, RegistrationService, WebHDFSApiClient}
import it.gov.daf.sso
import it.gov.daf.sso.{ApiClientIPA, Organization, RoleGroup, User}
import play.api.mvc.{Action, Controller, Result}
import security_manager.yaml.{IpaGroupMember_sys_user, IpaUserMod, Success}

import scala.concurrent.Future


// These services are only for migration or test pourposes
class MigrationController  @Inject()(apiClientIPA: ApiClientIPA,impalaService: ImpalaService, webHDFSApiClient: WebHDFSApiClient,registrationService: RegistrationService) extends Controller {

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


  def addUserToOpenGroup(userId:String) = Action.async { implicit request =>

    apiClientIPA.addMemberToGroups(Some(Seq(sso.OPEN_DATA_GROUP)), User(userId)).map {
      case Right(s) => Ok(s"user $userId updated")
      case Left(l) => InternalServerError(l.toString)
    }

  }

  def setDefaultViewerRole = Action.async { implicit request =>

    val json = request.body.asJson.get
    val userId = (json \ "userId").as[String]
    val orgName = (json \ "orgName").asOpt[String]


    if(orgName.nonEmpty && orgName.get.trim.size>0 ) {

      val orgViewerRoleName = Viewer.toString + orgName.get

      apiClientIPA.addMemberToGroups(Some(Seq(orgViewerRoleName)), User(userId)).map {
        case Right(s) => Ok(s"user $userId updated")
        case Left(l) => InternalServerError(l.toString)
      }

    }else
      Future.successful(Ok(s"user update of $userId skipped"))

  }


  def addOrgToOrgsGroup(orgName:String) = Action.async { implicit request =>

      apiClientIPA.addMembersToGroup(daf.sso.ORGANIZATIONS_GROUP, Organization(orgName)).map {
        case Right(s) => Ok(s"organization $orgName updated")
        case Left(l) => InternalServerError(l.toString)
      }

  }



  def setEditorRole= Action.async { implicit request =>

    val json = request.body.asJson.get
    val userId = (json \ "userId").as[String]
    val orgName = (json \ "orgName").as[String]

    val orgEditorRoleName = Editor.toString + orgName
    val orgViewerRoleName = Viewer.toString + orgName

    val userMod = IpaUserMod(None, None, Some(Seq(orgEditorRoleName)), Some(Seq(orgViewerRoleName)))

    val out = for{
      userInfo <- EitherT( apiClientIPA.findUser(Left(userId)) )
      a <- EitherT(
        if(!userInfo.roles.contains(orgEditorRoleName))
          registrationService.updateUser(userId, userMod)
        else
          Future.successful{Right( Success(Some("ok"), Some("ok")) )}
      )
    } yield a


    out.value.map{
      case Right(s) => Ok("done")
      case Left(l) => InternalServerError(l.toString)
    }

  }



}
