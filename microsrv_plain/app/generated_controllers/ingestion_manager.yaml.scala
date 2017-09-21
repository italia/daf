
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import java.io.File
import it.gov.daf.microsrv.ClientCaller

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package ingestion_manager.yaml {
    // ----- Start of unmanaged code area for package Ingestion_managerYaml
                                                                    
    // ----- End of unmanaged code area for package Ingestion_managerYaml
    class Ingestion_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Ingestion_managerYaml

        // ----- End of unmanaged code area for injections Ingestion_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Ingestion_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Ingestion_managerYaml


        // ----- End of unmanaged code area for constructor Ingestion_managerYaml
        val testmicrosrv = testmicrosrvAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.testmicrosrv
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.testmicrosrv
        }
    
     // Dead code for absent methodIngestion_managerYaml.testingestion
     /*
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.testingestion
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.testingestion
     */

    
     // Dead code for absent methodIngestion_managerYaml.addNewDataset
     /*
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addNewDataset

             NotImplementedYet
          //AddNewDataset200(res.toString)
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
     */

    
     // Dead code for absent methodIngestion_managerYaml.addDataset
     /*
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addDataset
            NotImplementedYet
      //AddDataset200(Successfull(Some("Dataset stored")))
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addDataset
     */

    
    }
}
