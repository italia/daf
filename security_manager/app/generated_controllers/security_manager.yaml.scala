
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import scala.concurrent.ExecutionContext.Implicits.global
import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.common.authentication.Authentication
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import it.gov.daf.securitymanager.service.RegistrationService
import scala.concurrent.Future
import it.gov.daf.sso.ApiClientIPA
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.common.sso.common.CacheWrapper
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.service.IntegrationService
import it.gov.daf.ftp.SftpHandler

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package security_manager.yaml {
    // ----- Start of unmanaged code area for package Security_managerYaml
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
    // ----- End of unmanaged code area for package Security_managerYaml
    class Security_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Security_managerYaml
                                           val configuration: Configuration,
                                           val playSessionStore: PlaySessionStore,
                                           cacheWrapper:CacheWrapper,
                                           apiClientIPA:ApiClientIPA,
                                           registrationService:RegistrationService,
                                           integrationService:IntegrationService,
        // ----- End of unmanaged code area for injections Security_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Security_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Security_managerYaml

      Authentication(configuration, playSessionStore)

    val sftpHost: String = configuration.underlying.getString("sftp.host")


        // ----- End of unmanaged code area for constructor Security_managerYaml
        val registrationconfirm = registrationconfirmAction { (token: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.registrationconfirm
            registrationService.createUser(token) flatMap {
              case Right(success) => Registrationconfirm200(success)
              case Left(err) => Registrationconfirm500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.registrationconfirm
        }
        val createDAFuser = createDAFuserAction { (user: IpaUser) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDAFuser
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
            CreateDAFuser500( Error(Option(1),Some("Admin permissions required"),None) )
          else
            registrationService.checkUserNcreate(user) flatMap {
              case Right(success) => CreateDAFuser200(success)
              case Left(err) => CreateDAFuser500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDAFuser
        }
        val createIPAgroup = createIPAgroupAction { (group: Group) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createIPAgroup
            /*
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              CreateIPAgroup500( Error(Option(1),Some("Admin permissions required"),None) )
            else
              apiClientIPA.createGroup(group.cn) flatMap {
                case Right(success) => CreateIPAgroup200(success)
                case Left(err) => CreateIPAgroup500(err)
              }*/
            CreateIPAgroup500(Error(Option(1),Some("The service is deprecated"),None))
            // ----- End of unmanaged code area for action  Security_managerYaml.createIPAgroup
        }
        val createDAForganization = createDAForganizationAction { (organization: DafOrg) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDAForganization
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              CreateDAForganization500( Error(Option(1),Some("Admin permissions required"),None) )
            else
              integrationService.createDafOrganization(organization)flatMap {
                case Right(success) => CreateDAForganization200(success)
                case Left(err) => CreateDAForganization500(err)
              }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDAForganization
        }
        val findIpauserByName = findIpauserByNameAction { (userName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.findIpauserByName
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

            apiClientIPA.findUserByUid(userName) flatMap {
              case Right(success) =>  if( success.uid == credentials.username || WebServiceUtil.isDafAdmin(currentRequest) )
                                        FindIpauserByName200(success)
                                      else
                                        FindIpauserByName500( Error(Option(1),Some("Permissions required"),None) )
              case Left(err) => FindIpauserByName500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.findIpauserByName
        }
        val deleteDAForganization = deleteDAForganizationAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.deleteDAForganization
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              DeleteDAForganization500( Error(Option(1),Some("Admin permissions required"),None) )
            else
              integrationService.deleteDafOrganization(orgName)flatMap {
                case Right(success) => DeleteDAForganization200(success)
                case Left(err) => DeleteDAForganization500(err)
              }
            // ----- End of unmanaged code area for action  Security_managerYaml.deleteDAForganization
        }
        val addUserToIPAgroup = addUserToIPAgroupAction { input: (String, UserList) =>
            val (group, users) = input
            // ----- Start of unmanaged code area for action  Security_managerYaml.addUserToIPAgroup
            /*
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              AddUserToIPAgroup500( Error(Option(1),Some("Admin permissions required"),None) )
            else
              apiClientIPA.addUsersToGroup(group,users.users.get) flatMap {
                case Right(success) => AddUserToIPAgroup200(success)
                case Left(err) => AddUserToIPAgroup500(err)
              }*/
            AddUserToIPAgroup500(Error(Option(1),Some("The service is deprecated"),None))
            // ----- End of unmanaged code area for action  Security_managerYaml.addUserToIPAgroup
        }
        val delUserToIPAgroup = delUserToIPAgroupAction { input: (String, UserList) =>
            val (group, users) = input
            // ----- Start of unmanaged code area for action  Security_managerYaml.delUserToIPAgroup
            /*
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              DelUserToIPAgroup500( Error(Option(1),Some("Admin permissions required"),None) )
            else
              apiClientIPA.removeUsersFromGroup(group,users.users.get) flatMap {
              case Right(success) => DelUserToIPAgroup200(success)
              case Left(err) => DelUserToIPAgroup500(err)
              }
              */
              DelUserToIPAgroup500(Error(Option(1),Some("The service is deprecated"),None))
            // ----- End of unmanaged code area for action  Security_managerYaml.delUserToIPAgroup
        }
        val createSupersetTable = createSupersetTableAction { (payload: SupersetTable) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createSupersetTable
            integrationService.createSupersetTable(payload.dbName,payload.schema,payload.tableName)flatMap {
              case Right(success) => CreateSupersetTable200(success)
              case Left(err) => CreateSupersetTable500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.createSupersetTable
        }
        val updateDAFuser = updateDAFuserAction { input: (String, IpaUserMod) =>
            val (uid, user) = input
            // ----- Start of unmanaged code area for action  Security_managerYaml.updateDAFuser
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
            UpdateDAFuser500( Error(Option(1),Some("Admin permissions required"),None) )
          else
            registrationService.updateUser(input._1,input._2.givenname,input._2.sn,input._2.role) flatMap {
              case Right(success) => UpdateDAFuser200(success)
              case Left(err) => UpdateDAFuser500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.updateDAFuser
        }
        val token = tokenAction {  _ =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.token
            //val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            //cacheWrapper.deleteCredentials(credentials._1.get)
            //cacheWrapper.putCredentials(credentials._1.get,credentials._2.get)

            Token200(Authentication.getStringToken(currentRequest, ConfigReader.tokenExpiration).getOrElse(""))
            // ----- End of unmanaged code area for action  Security_managerYaml.token
        }
        val showipagroup = showipagroupAction { (cn: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.showipagroup
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

            if( credentials.groups.contains(cn) || WebServiceUtil.isDafAdmin(currentRequest) )
              apiClientIPA.showGroup(cn) flatMap {
                case Right(success) => Showipagroup200(success)
                case Left(err) => Showipagroup500(err)
              }
            else
              Showipagroup500( Error(Option(1),Some("Admin permissions required"),None) )
            // ----- End of unmanaged code area for action  Security_managerYaml.showipagroup
        }
        val useraddDAForganization = useraddDAForganizationAction { (payload: UserAndGroup) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.useraddDAForganization
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

            if( (credentials.groups.contains(payload.groupCn) && WebServiceUtil.isDafEditor(currentRequest) ) ||
                WebServiceUtil.isDafAdmin(currentRequest) )
              integrationService.addUserToOrganization( payload.groupCn, payload.userId )flatMap {
                case Right(success) => UseraddDAForganization200(success)
                case Left(err) => UseraddDAForganization500(err)
              }
            else
              UseraddDAForganization500( Error(Option(1),Some("Permissions required"),None) )
            // ----- End of unmanaged code area for action  Security_managerYaml.useraddDAForganization
        }
        val findIpauserByMail = findIpauserByMailAction { (mail: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.findIpauserByMail
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

            apiClientIPA.findUserByMail(mail) flatMap {

              case Right(success) => if( success.uid == credentials.username || WebServiceUtil.isDafAdmin(currentRequest) )
                                        FindIpauserByMail200(success)
                                      else
                                        FindIpauserByMail500( Error(Option(1),Some("Permissions required"),None) )

              case Left(err) => FindIpauserByMail500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.findIpauserByMail
        }
        val createDefaultDAForganization = createDefaultDAForganizationAction { (organization: DafOrg) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDefaultDAForganization
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              CreateDefaultDAForganization500( Error(Option(1),Some("Admin permissions required"),None) )
            else
              integrationService.createDefaultDafOrganization(organization.predefinedUserPwd)flatMap {
                case Right(success) => CreateDefaultDAForganization200(success)
                case Left(err) => CreateDefaultDAForganization500(err)
              }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDefaultDAForganization
        }
        val sftp = sftpAction { (path_to_create: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.sftp
            val user_id = WebServiceUtil.readCredentialFromRequest(currentRequest).username
            val tryPwd: Try[String]= cacheWrapper.getPwd(user_id) match {
            case Some(path) => scala.util.Success(path)
            case None => scala.util.Failure(new Throwable(s"cannot find user id $user_id"))
          }

          val result = tryPwd.flatMap { pwd =>
            val sftp = new SftpHandler(user_id, pwd, sftpHost)
            sftp.mkdir(path_to_create)
          }

          result match {
            case scala.util.Success(path) => Sftp200(path)
            case scala.util.Failure(ex) => Sftp500(Error(Some(404), Some(ex.getMessage), None))
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.sftp
        }
        val findSupersetOrgTables = findSupersetOrgTablesAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.findSupersetOrgTables
            integrationService.getSupersetOrgTables(orgName)flatMap {
              case Right(success) => FindSupersetOrgTables200(SupersetTables(Some(success)))
              case Left(err) => FindSupersetOrgTables500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.findSupersetOrgTables
        }
        val listDAForganization = listDAForganizationAction {  _ =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.listDAForganization
            apiClientIPA.organizationList()flatMap {
              case Right(success) => ListDAForganization200(OrgList(success))
              case Left(err) => ListDAForganization500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.listDAForganization
        }
        val deleteDAFuser = deleteDAFuserAction { (userName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.deleteDAFuser
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
            DeleteDAFuser500( Error(Option(1),Some("Admin permissions required"),None) )
          else
            registrationService.deleteUser(userName) flatMap {
              case Right(success) => DeleteDAFuser200(success)
              case Left(err) => DeleteDAFuser500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.deleteDAFuser
        }
        val userdelDAForganization = userdelDAForganizationAction { (payload: UserAndGroup) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.userdelDAForganization
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

          if( (credentials.groups.contains(payload.groupCn) && WebServiceUtil.isDafEditor(currentRequest) ) ||
            WebServiceUtil.isDafAdmin(currentRequest) )
            integrationService.removeUserFromOrganization( payload.groupCn, payload.userId )flatMap {
              case Right(success) => UserdelDAForganization200(success)
              case Left(err) => UserdelDAForganization500(err)
            }
          else
            UserdelDAForganization500( Error(Option(1),Some("Permissions required"),None) )
            // ----- End of unmanaged code area for action  Security_managerYaml.userdelDAForganization
        }
        val registrationrequest = registrationrequestAction { (user: IpaUser) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.registrationrequest
            val reg = registrationService.requestRegistration(user) flatMap {
              case Right(mailService) => mailService.sendMail()
              case Left(msg) => Future {Left(msg)}
            }

            reg flatMap {
              case Right(msg) => Registrationrequest200(Success(Some("Success"), Some(msg)))
              case Left(msg) => Registrationrequest500(Error(Option(1), Option(msg), None))
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.registrationrequest
        }
    
    }
}
