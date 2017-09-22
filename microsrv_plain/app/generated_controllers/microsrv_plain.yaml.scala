
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

package microsrv_plain.yaml {
    // ----- Start of unmanaged code area for package Microsrv_plainYaml
                
    // ----- End of unmanaged code area for package Microsrv_plainYaml
    class Microsrv_plainYaml @Inject() (
        // ----- Start of unmanaged code area for injections Microsrv_plainYaml

        // ----- End of unmanaged code area for injections Microsrv_plainYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Microsrv_plainYamlBase {
        // ----- Start of unmanaged code area for constructor Microsrv_plainYaml

        // ----- End of unmanaged code area for constructor Microsrv_plainYaml
        val testmicrosrv = testmicrosrvAction {  _ =>  
            // ----- Start of unmanaged code area for action  Microsrv_plainYaml.testmicrosrv
            NotImplementedYet
            // ----- End of unmanaged code area for action  Microsrv_plainYaml.testmicrosrv
        }
    
    }
}
