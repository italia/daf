
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import scala.math.BigInt

import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.server.storage.DatasetManagerService
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext

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
        val getDataset = getDatasetAction { (datasetId: String) =>  
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.getDataset
            //FIXME add authorization
      GetDataset200(service.getDataset("", datasetId))
            // ----- End of unmanaged code area for action  Dataset_managerYaml.getDataset
        }
        val getDatasetSchema = getDatasetSchemaAction { (datasetId: String) =>  
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.getDatasetSchema
            //FIXME add authorization
      GetDatasetSchema200(service.getDatasetSchema("", datasetId))
            // ----- End of unmanaged code area for action  Dataset_managerYaml.getDatasetSchema
        }
        val getDatasetLimit = getDatasetLimitAction { input: (String, BigInt) =>
            val (datasetId, size) = input
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.getDatasetLimit
            //FIXME add authorization
      GetDatasetLimit200(service.getDataset("", datasetId, size.toInt))
            // ----- End of unmanaged code area for action  Dataset_managerYaml.getDatasetLimit
        }
        val searchDataset = searchDatasetAction { input: (String, Query) =>
            val (datasetId, query) = input
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.searchDataset
            //FIXME add authorization
      SearchDataset200(service.searchDataset("", datasetId, query))
            // ----- End of unmanaged code area for action  Dataset_managerYaml.searchDataset
        }
    
     // Dead code for absent methodDataset_managerYaml.createIPAuser
     /*
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.createIPAuser
            NotImplementedYet
            // ----- End of unmanaged code area for action  Dataset_managerYaml.createIPAuser
     */

    
     // Dead code for absent methodDataset_managerYaml.token
     /*
            // ----- Start of unmanaged code area for action  Dataset_managerYaml.token
            NotImplementedYet
            // ----- End of unmanaged code area for action  Dataset_managerYaml.token
     */

    
    }
}
