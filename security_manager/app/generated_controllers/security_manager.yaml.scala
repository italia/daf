
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

        // ----- End of unmanaged code area for constructor Security_managerYaml
        val registrationconfirm = registrationconfirmAction { (token: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.registrationconfirm
            registrationService.createUser(token) flatMap {
              case Right(success) => Registrationconfirm200(success)
              case Left(err) => Registrationconfirm500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.registrationconfirm
        }
        val createIPAuser = createIPAuserAction { (user: IpaUser) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createIPAuser
            apiClientIPA.createUser(user) flatMap {
              case Right(success) => CreateIPAuser200(success)
              case Left(err) => CreateIPAuser500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.createIPAuser
        }
        val createIPAgroup = createIPAgroupAction { (group: Group) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createIPAgroup
            apiClientIPA.createGroup(group.cn) flatMap {
              case Right(success) => CreateIPAgroup200(success)
              case Left(err) => CreateIPAgroup500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.createIPAgroup
        }
        val createDAForganization = createDAForganizationAction { (organization: DafOrg) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createDAForganization
            integrationService.createDafOrganization(organization)flatMap {
              case Right(success) => CreateDAForganization200(success)
              case Left(err) => CreateDAForganization500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.createDAForganization
        }
        val deleteDAForganization = deleteDAForganizationAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.deleteDAForganization
            integrationService.deleteDafOrganization(orgName)flatMap {
              case Right(success) => DeleteDAForganization200(success)
              case Left(err) => DeleteDAForganization500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.deleteDAForganization
        }
        val useraddMyDAForganization = useraddMyDAForganizationAction { (payload: Username) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.useraddMyDAForganization

            integrationService.addUserToOrganization("todo",payload.userId)flatMap {// TODO
              case Right(success) => UseraddMyDAForganization200(success)
              case Left(err) => UseraddMyDAForganization500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.useraddMyDAForganization
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
            integrationService.addUserToOrganization(payload.groupCn,payload.userId)flatMap {
              case Right(success) => UseraddDAForganization200(success)
              case Left(err) => UseraddDAForganization500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.useraddDAForganization
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
        val addUserToIPAgroup = addUserToIPAgroupAction { input: (String, UserList) =>
            val (group, users) = input
            // ----- Start of unmanaged code area for action  Security_managerYaml.addUserToIPAgroup
            apiClientIPA.addUsersToGroup(group,users) flatMap {
              case Right(success) => AddUserToIPAgroup200(success)
              case Left(err) => AddUserToIPAgroup500(err)
            }
            // ----- End of unmanaged code area for action  Security_managerYaml.addUserToIPAgroup
        }
    
    }
}
