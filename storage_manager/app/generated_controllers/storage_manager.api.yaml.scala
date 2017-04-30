
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

package storage_manager.api.yaml {
    // ----- Start of unmanaged code area for package Storage_managerApiYaml
        
    // ----- End of unmanaged code area for package Storage_managerApiYaml
    class Storage_managerApiYaml @Inject() (
        // ----- Start of unmanaged code area for injections Storage_managerApiYaml
                                          val withFileSystem: WithFileSystem,
        // ----- End of unmanaged code area for injections Storage_managerApiYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Storage_managerApiYamlBase {
        // ----- Start of unmanaged code area for constructor Storage_managerApiYaml

        // ----- End of unmanaged code area for constructor Storage_managerApiYaml
        val getdataset = getdatasetAction { (uri: String) =>  
            // ----- Start of unmanaged code area for action  Storage_managerApiYaml.getdataset
            println(withFileSystem.fs)
      NotImplementedYet
            // ----- End of unmanaged code area for action  Storage_managerApiYaml.getdataset
        }
    
    }
}
