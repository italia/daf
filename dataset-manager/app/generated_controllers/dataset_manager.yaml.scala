
import javax.inject._

import de.zalando.play.controllers.PlayBodyParsing._
import de.zalando.play.controllers._
import play.api.i18n.MessagesApi
import play.api.inject.{ApplicationLifecycle, ConfigurationProvider}

import scala.math.BigInt

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package dataset_manager.yaml {


  // ----- Start of unmanaged code area for package Dataset_managerYaml
  import it.gov.daf.server.DatasetManagerService
  import play.api.libs.ws.WSClient
  import scala.concurrent.ExecutionContext
  // ----- End of unmanaged code area for package Dataset_managerYaml
  class Dataset_managerYaml @Inject() (
    // ----- Start of unmanaged code area for injections Dataset_managerYaml
    ws: WSClient,
    implicit val ex: ExecutionContext,
    // ----- End of unmanaged code area for injections Dataset_managerYaml
    val messagesApi: MessagesApi,
    lifecycle: ApplicationLifecycle,
    config: ConfigurationProvider
  ) extends Dataset_managerYamlBase {
    // ----- Start of unmanaged code area for constructor Dataset_managerYaml
    private val catalogUrl = config.get.underlying.getString("daf.catalogUrl")
    private val storageUrl = config.get.underlying.getString("daf.storageUrl")
    private val service = new DatasetManagerService(
      catalogUrl = catalogUrl,
      storageUrl = storageUrl,
      ws = ws
    )

    // ----- End of unmanaged code area for constructor Dataset_managerYaml
    val getDataset = getDatasetAction { (datasetId: String) =>
      // ----- Start of unmanaged code area for action  Dataset_managerYaml.getDataset
      //FIXME add authorization
      //GetDataset200(service.getDataset("", datasetId))
      NotImplementedYet
      // ----- End of unmanaged code area for action  Dataset_managerYaml.getDataset
    }
    val getDatasetSchema = getDatasetSchemaAction { (datasetId: String) =>
      // ----- Start of unmanaged code area for action  Dataset_managerYaml.getDatasetSchema
      //FIXME add authorization
      //GetDatasetSchema200(service.getDatasetSchema("", datasetId))
      NotImplementedYet
      // ----- End of unmanaged code area for action  Dataset_managerYaml.getDatasetSchema
    }
    val getDatasetLimit = getDatasetLimitAction { input: (String, BigInt) =>
      val (datasetId, size) = input
      // ----- Start of unmanaged code area for action  Dataset_managerYaml.getDatasetLimit
      //FIXME add authorization
      //GetDatasetLimit200(service.getDataset("", datasetId, size.toInt))
      NotImplementedYet
      // ----- End of unmanaged code area for action  Dataset_managerYaml.getDatasetLimit
    }
    val searchDataset = searchDatasetAction { input: (String, Query) =>
      val (datasetId, query) = input
      // ----- Start of unmanaged code area for action  Dataset_managerYaml.searchDataset
      //FIXME add authorization
      //SearchDataset200(service.searchDataset("", datasetId, query))
      NotImplementedYet
      // ----- End of unmanaged code area for action  Dataset_managerYaml.searchDataset
    }
  }
}
