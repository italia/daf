
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

package iot_manager.yaml {
    // ----- Start of unmanaged code area for package Iot_managerYaml
                        
    // ----- End of unmanaged code area for package Iot_managerYaml
    class Iot_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Iot_managerYaml

        // ----- End of unmanaged code area for injections Iot_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Iot_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Iot_managerYaml

        // ----- End of unmanaged code area for constructor Iot_managerYaml
        val token = tokenAction {  _ =>  
            // ----- Start of unmanaged code area for action  Iot_managerYaml.token
            //NotImplementedYet
            Token200("")
            // ----- End of unmanaged code area for action  Iot_managerYaml.token
        }
    
    }
}
