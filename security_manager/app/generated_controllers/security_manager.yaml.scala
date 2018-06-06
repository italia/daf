
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.common.authentication.Authentication
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import it.gov.daf.securitymanager.service.RegistrationService
import scala.concurrent.Future
import it.gov.daf.sso.ApiClientIPA
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.securitymanager.service.IntegrationService
import it.gov.daf.ftp.SftpHandler
import it.gov.daf.common.sso.common.CredentialManager
import it.gov.daf.securitymanager.service.utilities.RequestContext._
import it.gov.daf.common.sso.common.CacheWrapper
import it.gov.daf.securitymanager.service.utilities.Utils

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
                                           apiClientIPA:ApiClientIPA,
                                           registrationService:RegistrationService,
                                           integrationService:IntegrationService,
                                            cacheWrapper: CacheWrapper,
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
            execInContext[Future[RegistrationconfirmType[T] forSome { type T }]] ("registrationconfirm"){ () =>
            registrationService.createUser(token) flatMap {
              case Right(success) => Registrationconfirm200(success)
              case Left(err) => Registrationconfirm500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.registrationconfirm
        }
        val createDAFuser = createDAFuserAction { (user: IpaUser) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDAFuser
            execInContext[Future[CreateDAFuserType[T] forSome { type T }]] ("createDAFuser"){ () =>
            if (!CredentialManager.isDafAdmin(currentRequest))
              CreateDAFuser500(Error(Option(1), Some("Admin permissions required"), None))
            else
              registrationService.checkUserNcreate(user) flatMap {
                case Right(success) => CreateDAFuser200(success)
                case Left(err) => CreateDAFuser500(err)
              }
          }
          //def a() = {registrationService.checkUserNcreate(user)}
            // ----- End of unmanaged code area for action  Security_managerYaml.createDAFuser
        }
        val resetpwdconfirm = resetpwdconfirmAction { (resetinfo: ConfirmResetPwdPayload) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.resetpwdconfirm
            execInContext[Future[ResetpwdconfirmType[T] forSome { type T }]] ("resetpwdconfirm"){ () =>
              registrationService.resetPassword(resetinfo.token, resetinfo.newpwd) flatMap {
                case Right(success) => Resetpwdconfirm200(success)
                case Left(err) => Resetpwdconfirm500(err)
              }
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.resetpwdconfirm
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

            /*
            apiClientIPA.testH.flatMap{
              case Right(success) => CreateIPAgroup200(success)
              case Left(err) => CreateIPAgroup500(err)

            }*/

            //CreateIPAgroup500(Error(Option(1),Some("The service is deprecated"),None))
            // ----- End of unmanaged code area for action  Security_managerYaml.createIPAgroup
        }
        val createDAForganization = createDAForganizationAction { (organization: DafOrg) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDAForganization
            execInContext[Future[CreateDAForganizationType[T] forSome { type T }]] ("createDAForganization") { () =>
            if (!CredentialManager.isDafAdmin(currentRequest))
              CreateDAForganization500(Error(Option(1), Some("Admin permissions required"), None))
            else
              integrationService.createDafOrganization(organization) flatMap {
                case Right(success) => CreateDAForganization200(success)
                case Left(err) => CreateDAForganization500(err)
              }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDAForganization
        }
        val findIpauserByName = findIpauserByNameAction { (userName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.findIpauserByName
            execInContext[Future[FindIpauserByNameType[T] forSome { type T }]]("findIpauserByName") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            apiClientIPA.findUserByUid(userName) flatMap {
              case Right(success) =>  if (success.uid == credentials.username || CredentialManager.isDafAdmin(currentRequest))
                                        FindIpauserByName200(success)
                                      else
                                        FindIpauserByName500(Error(Option(1), Some("Permissions required"), None))
              case Left(err) => FindIpauserByName500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.findIpauserByName
        }
        val deleteDAForganization = deleteDAForganizationAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.deleteDAForganization
            execInContext[Future[DeleteDAForganizationType[T] forSome { type T }]] ("deleteDAForganization"){ () =>
            if (!CredentialManager.isDafAdmin(currentRequest))
              DeleteDAForganization500(Error(Option(1), Some("Admin permissions required"), None))
            else
              integrationService.deleteDafOrganization(orgName) flatMap {
                case Right(success) => DeleteDAForganization200(success)
                case Left(err) => DeleteDAForganization500(err)
              }
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
            execInContext[Future[CreateSupersetTableType[T] forSome { type T }]] ("createSupersetTable"){ () =>
            integrationService.createSupersetTable(payload.dbName, payload.schema, payload.tableName) flatMap {
              case Right(success) => CreateSupersetTable200(success)
              case Left(err) => CreateSupersetTable500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.createSupersetTable
        }
        val updateDAFuser = updateDAFuserAction { input: (String, IpaUserMod) =>
            val (uid, user) = input
            // ----- Start of unmanaged code area for action  Security_managerYaml.updateDAFuser
            execInContext[Future[UpdateDAFuserType[T] forSome { type T }]] ("updateDAFuser"){ () =>
            if (!CredentialManager.isDafAdmin(currentRequest))
              UpdateDAFuser500(Error(Option(1), Some("Admin permissions required"), None))
            else
              registrationService.updateUser(input._1, input._2.givenname, input._2.sn, input._2.role) flatMap {
                case Right(success) => UpdateDAFuser200(success)
                case Left(err) => UpdateDAFuser500(err)
              }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.updateDAFuser
        }
        val token = tokenAction {  _ =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.token
            //val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            //cacheWrapper.deleteCredentials(credentials._1.get)
            //cacheWrapper.putCredentials(credentials._1.get,credentials._2.get)
          execInContext[Future[TokenType[T] forSome { type T }]] ("token"){ () =>
            Token200(Authentication.getStringToken(currentRequest, ConfigReader.tokenExpiration).getOrElse(""))
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.token
        }
        val showipagroup = showipagroupAction { (cn: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.showipagroup
            execInContext[Future[ShowipagroupType[T] forSome { type T }]] ("showipagroup"){ () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            if (credentials.groups.contains(cn) || CredentialManager.isDafAdmin(currentRequest))
              apiClientIPA.showGroup(cn) flatMap {
                case Right(success) => Showipagroup200(success)
                case Left(err) => Showipagroup500(err)
              }
            else
              Showipagroup500(Error(Option(1), Some("Admin permissions required"), None))
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.showipagroup
        }
        val useraddDAForganization = useraddDAForganizationAction { (payload: UserAndGroup) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.useraddDAForganization
            execInContext[Future[UseraddDAForganizationType[T] forSome { type T }]] ("useraddDAForganization"){ () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            if ((credentials.groups.contains(payload.groupCn) && CredentialManager.isDafEditor(currentRequest)) ||
              CredentialManager.isDafAdmin(currentRequest))
              integrationService.addUserToOrganization(payload.groupCn, payload.userId) flatMap {
                case Right(success) => UseraddDAForganization200(success)
                case Left(err) => UseraddDAForganization500(err)
              }
            else
              UseraddDAForganization500(Error(Option(1), Some("Permissions required"), None))
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.useraddDAForganization
        }
        val findIpauserByMail = findIpauserByMailAction { (mail: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.findIpauserByMail
            execInContext[Future[FindIpauserByMailType[T] forSome { type T }]] ("findIpauserByMail"){ () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            apiClientIPA.findUserByMail(mail) flatMap {

              case Right(success) =>  if (success.uid == credentials.username || CredentialManager.isDafAdmin(currentRequest))
                                        FindIpauserByMail200(success)
                                      else
                                        FindIpauserByMail500(Error(Option(1), Some("Permissions required"), None))

              case Left(err) => FindIpauserByMail500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.findIpauserByMail
        }
        val createDefaultDAForganization = createDefaultDAForganizationAction { (organization: DafOrg) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDefaultDAForganization
            execInContext[Future[CreateDefaultDAForganizationType[T] forSome { type T }]] ("createDefaultDAForganization"){ () =>
            if (!CredentialManager.isDafAdmin(currentRequest))
              CreateDefaultDAForganization500(Error(Option(1), Some("Admin permissions required"), None))
            else
              integrationService.createDefaultDafOrganization(organization.predefinedUserPwd) flatMap {
                case Right(success) => CreateDefaultDAForganization200(success)
                case Left(err) => CreateDefaultDAForganization500(err)
              }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDefaultDAForganization
        }
        val sftp = sftpAction { (path_to_create: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.sftp
            execInContext[Future[SftpType[T] forSome { type T }]] ("sftp"){ () =>
            val credentials = Utils.getCredentials(currentRequest, cacheWrapper )

            if (CredentialManager.isDafAdmin(currentRequest) || CredentialManager.isDafEditor(currentRequest)) {
              val result = credentials.flatMap { crd =>
                val sftp = new SftpHandler(crd.username, crd.password, sftpHost)
                sftp.mkdir(path_to_create)
              }

              result match {
                case scala.util.Success(path) => Sftp200(path)
                case scala.util.Failure(ex) => Sftp500(Error(Some(404), Some(ex.getMessage), None))
              }
            } else
              Sftp500(Error(Option(1), Some("Permissions required"), None))
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.sftp
        }
        val findSupersetOrgTables = findSupersetOrgTablesAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.findSupersetOrgTables
            execInContext[Future[FindSupersetOrgTablesType[T] forSome { type T }]] ("findSupersetOrgTables"){ () =>
            integrationService.getSupersetOrgTables(orgName) flatMap {
              case Right(success) => FindSupersetOrgTables200(SupersetTables(Some(success)))
              case Left(err) => FindSupersetOrgTables500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.findSupersetOrgTables
        }
        val resetpwdrequest = resetpwdrequestAction { (resetMail: ResetPwdPayload) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.resetpwdrequest
            execInContext[Future[ResetpwdrequestType[T] forSome { type T }]] ("resetpwdrequest"){ () =>
            registrationService.requestResetPwd(resetMail.mail) flatMap {
              case Right(mailService) => mailService.sendResetPwdMail()
              case Left(e) => Future(Left(e))
            } flatMap {
              case Right(s) => Resetpwdrequest200(s)
              case Left(err) => Resetpwdrequest500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.resetpwdrequest
        }
        val listDAForganization = listDAForganizationAction {  _ =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.listDAForganization
            execInContext[Future[ListDAForganizationType[T] forSome { type T }]] ("listDAForganization"){ () =>
            apiClientIPA.organizationList() flatMap {
              case Right(success) => ListDAForganization200(OrgList(success))
              case Left(err) => ListDAForganization500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.listDAForganization
        }
        val deleteDAFuser = deleteDAFuserAction { (userName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.deleteDAFuser
            execInContext[Future[DeleteDAFuserType[T] forSome { type T }]] ("deleteDAFuser"){ () =>
            if (!CredentialManager.isDafAdmin(currentRequest))
              DeleteDAFuser500(Error(Option(1), Some("Admin permissions required"), None))
            else
              registrationService.deleteUser(userName) flatMap {
                case Right(success) => DeleteDAFuser200(success)
                case Left(err) => DeleteDAFuser500(err)
              }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.deleteDAFuser
        }
        val userdelDAForganization = userdelDAForganizationAction { (payload: UserAndGroup) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.userdelDAForganization
            execInContext[Future[UserdelDAForganizationType[T] forSome { type T }]] ("userdelDAForganization"){ () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            if ((credentials.groups.contains(payload.groupCn) && CredentialManager.isDafEditor(currentRequest)) ||
              CredentialManager.isDafAdmin(currentRequest))
              integrationService.removeUserFromOrganization(payload.groupCn, payload.userId) flatMap {
                case Right(success) => UserdelDAForganization200(success)
                case Left(err) => UserdelDAForganization500(err)
              }
            else
              UserdelDAForganization500(Error(Option(1), Some("Permissions required"), None))
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.userdelDAForganization
        }
        val registrationrequest = registrationrequestAction { (user: IpaUser) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.registrationrequest
            execInContext[Future[RegistrationrequestType[T] forSome { type T }]] ("registrationrequest"){ () =>
            registrationService.requestRegistration(user) flatMap {
              case Right(mailService) => mailService.sendRegistrationMail()
              case Left(e) => Future( Left(e) )
            } flatMap {
              case Right(s) => Registrationrequest200(s)
              case Left(err) => Registrationrequest500(err)
            }
          }
            // ----- End of unmanaged code area for action  Security_managerYaml.registrationrequest
        }
    
    }
}
