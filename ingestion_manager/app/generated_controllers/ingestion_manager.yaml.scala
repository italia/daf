
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

import com.typesafe.config.ConfigFactory
import it.teamDigitale.daf.ingestion.IngestionManager
import it.teamDigitale.daf.schemamanager.SchemaManager
import it.gov.daf.catalogmanagerclient.api.DatasetCatalogApi
import it.gov.daf.catalogmanagerclient.invoker.ApiInvoker
import it.gov.daf.catalogmanagerclient.model.MetaCatalog

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
        val ingestionManager = new IngestionManager()
        val uriCatalogManager = ConfigFactory.load().getString("WebServices.catalogUrl")
        val dm = new SchemaManager

        val invoker = new ApiInvoker()
        //val client = new JWTTokenApi(defBasePath = s"http://localhost:$port/security-manager/v1", defApiInvoker = invoker)
        val catalogManagerApi: DatasetCatalogApi = new DatasetCatalogApi(defApiInvoker = invoker)




        // ----- End of unmanaged code area for constructor Ingestion_managerYaml
        val testingestion = testingestionAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.testingestion
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.testingestion
        }
        val addDataset = addDatasetAction { input: (File, String) =>
            val (upfile, uri) = input
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addDataset
            println("")
            val schema: Option[MetaCatalog] = catalogManagerApi.datasetcatalogbyid(uri)

           // val tryschema: Try[Schema] = dm.getSchemaFromUri(uriCatalogManager, uri)

            val res: Option[Boolean] = schema.map(s =>  ingestionManager.write(s))

            val httpres= res match {
                case Some(true) => Successfull(Some("Dataset stored"))
                case Some(false) => Successfull(Some("ERROR dataset cannot be stored"))
                case None => Successfull(Some(s"ERROR dataset cannot be stored"))
            }
            AddDataset200(httpres)
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addDataset
        }
    
    }
}
