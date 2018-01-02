
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
            CreateDAFuser500( Error(Option(0),Some("Admin permissions required"),None) )
          else
            registrationService.checkUserNcreate(user) flatMap {
              case Right(success) => CreateDAFuser200(success)
              case Left(err) => CreateDAFuser500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDAFuser
        }
        val createIPAgroup = createIPAgroupAction { (group: Group) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createIPAgroup
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              CreateIPAgroup500( Error(Option(0),Some("Admin permissions required"),None) )
            else
              apiClientIPA.createGroup(group.cn) flatMap {
                case Right(success) => CreateIPAgroup200(success)
                case Left(err) => CreateIPAgroup500(err)
              }
            // ----- End of unmanaged code area for action  Security_managerYaml.createIPAgroup
        }
        val createDAForganization = createDAForganizationAction { (organization: DafOrg) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDAForganization
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              CreateDAForganization500( Error(Option(0),Some("Admin permissions required"),None) )
            else
              integrationService.createDafOrganization(organization)flatMap {
                case Right(success) => CreateDAForganization200(success)
                case Left(err) => CreateDAForganization500(err)
              }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDAForganization
        }
        val deleteDAForganization = deleteDAForganizationAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.deleteDAForganization
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              DeleteDAForganization500( Error(Option(0),Some("Admin permissions required"),None) )
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
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              AddUserToIPAgroup500( Error(Option(0),Some("Admin permissions required"),None) )
            else
              apiClientIPA.addUsersToGroup(group,users) flatMap {
                case Right(success) => AddUserToIPAgroup200(success)
                case Left(err) => AddUserToIPAgroup500(err)
              }
            // ----- End of unmanaged code area for action  Security_managerYaml.addUserToIPAgroup
        }
        val delUserToIPAgroup = delUserToIPAgroupAction { input: (String, UserList) =>
            val (group, users) = input
            // ----- Start of unmanaged code area for action  Security_managerYaml.delUserToIPAgroup
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              DelUserToIPAgroup500( Error(Option(0),Some("Admin permissions required"),None) )
            else
              apiClientIPA.removeUsersFromGroup(group,users) flatMap {
              case Right(success) => DelUserToIPAgroup200(success)
              case Left(err) => DelUserToIPAgroup500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.delUserToIPAgroup
        }
        val sftp = sftpAction { input: (String, String) =>
            val (user_id, path_to_create) = input
            // ----- Start of unmanaged code area for action  Security_managerYaml.sftp
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
        val token = tokenAction {  _ =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.token
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            //SsoServiceClient.registerInternal(credentials._1.get,credentials._2.get)
            cacheWrapper.putCredentials(credentials._1.get,credentials._2.get)

            Token200(Authentication.getStringToken(currentRequest, ConfigReader.tokenExpiration).getOrElse(""))
            // ----- End of unmanaged code area for action  Security_managerYaml.token
        }
        val useraddDAForganization = useraddDAForganizationAction { (payload: UserAndGroup) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.useraddDAForganization
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

            if( credentials._3.contains(payload.groupCn) || WebServiceUtil.isDafAdmin(currentRequest) )
              integrationService.addUserToOrganization( payload.groupCn, payload.userId )flatMap {
                case Right(success) => UseraddDAForganization200(success)
                case Left(err) => UseraddDAForganization500(err)
              }
            else
              UseraddDAForganization500( Error(Option(0),Some("Admin permissions required"),None) )
            // ----- End of unmanaged code area for action  Security_managerYaml.useraddDAForganization
        }
        val createDefaultDAForganization = createDefaultDAForganizationAction { (organization: DafOrg) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDefaultDAForganization
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
            CreateDefaultDAForganization500( Error(Option(0),Some("Admin permissions required"),None) )
          else
            integrationService.createDefaultDafOrganization()flatMap {
              case Right(success) => CreateDefaultDAForganization200(success)
              case Left(err) => CreateDefaultDAForganization500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDefaultDAForganization
        }
        val showipauser = showipauserAction { (mail: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.showipauser
            apiClientIPA.findUserByMail(mail) flatMap {
              case Right(success) => Showipauser200(success)
              case Left(err) => Showipauser500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.showipauser
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
    
     // Dead code for absent methodSecurity_managerYaml.createIPAuser
     /*
            // ----- Start of unmanaged code area for action  Security_managerYaml.createIPAuser
            if(! WebServiceUtil.isDafAdmin(currentRequest) )
              CreateIPAuser500( Error(Option(0),Some("Admin permissions required"),None) )
            else
              apiClientIPA.createUser(user) flatMap {
                case Right(success) => CreateIPAuser200(success)
                case Left(err) => CreateIPAuser500(err)
              }
            // ----- End of unmanaged code area for action  Security_managerYaml.createIPAuser
     */

    
    }
}
