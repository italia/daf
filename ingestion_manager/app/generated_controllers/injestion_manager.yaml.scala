
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import it.gov.daf.injestionmanager.Test

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package injestion_manager.yaml {
    // ----- Start of unmanaged code area for package Injestion_managerYaml
                
    // ----- End of unmanaged code area for package Injestion_managerYaml
    class Injestion_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Injestion_managerYaml

        // ----- End of unmanaged code area for injections Injestion_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Injestion_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Injestion_managerYaml

        // ----- End of unmanaged code area for constructor Injestion_managerYaml
        val testinjestion = testinjestionAction {  _ =>  
            // ----- Start of unmanaged code area for action  Injestion_managerYaml.testinjestion
            println("Ale")
            Test.test
            NotImplementedYet
            // ----- End of unmanaged code area for action  Injestion_managerYaml.testinjestion
        }
    
    }
}
