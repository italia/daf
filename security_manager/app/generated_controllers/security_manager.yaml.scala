
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.common.authentication.Authentication
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration

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
        // ----- End of unmanaged code area for injections Security_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Security_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Security_managerYaml

    Authentication(configuration, playSessionStore)

    @SuppressWarnings(
      Array(
        "org.wartremover.warts.StringPlusAny",
        "org.wartremover.warts.NonUnitStatements"
      )
    )
        // ----- End of unmanaged code area for constructor Security_managerYaml
        val token = tokenAction {  _ =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.token
            Token200(Authentication.getStringToken(current_request_for_tokenAction).getOrElse(""))
            // ----- End of unmanaged code area for action  Security_managerYaml.token
        }
        val createIPAuser = createIPAuserAction { (user: User) =>  
            // ----- Start of unmanaged code area for action  Security_managerYaml.createIPAuser
            NotImplementedYet
            // ----- End of unmanaged code area for action  Security_managerYaml.createIPAuser
        }
    
    }
}
