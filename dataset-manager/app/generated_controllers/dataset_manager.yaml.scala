
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._


/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package dataset_manager.yaml {
    // ----- Start of unmanaged code area for package Dataset_managerYaml
                    
    // ----- End of unmanaged code area for package Dataset_managerYaml
    class Dataset_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Dataset_managerYaml

        // ----- End of unmanaged code area for injections Dataset_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Dataset_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Dataset_managerYaml

        // ----- End of unmanaged code area for constructor Dataset_managerYaml
        val token = tokenAction {  _ =>  
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.token
            NotImplementedYet
            // ----- End of unmanaged code area for action  Dataset_managerYaml.token
        }
        val createIPAuser = createIPAuserAction { (user: UserIpa) =>  
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.createIPAuser
            NotImplementedYet
            // ----- End of unmanaged code area for action  Dataset_managerYaml.createIPAuser
        }
    
    }
}
