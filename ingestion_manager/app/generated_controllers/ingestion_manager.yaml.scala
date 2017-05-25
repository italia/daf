
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


        // ----- End of unmanaged code area for constructor Ingestion_managerYaml
        val testingestion = testingestionAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.testingestion
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.testingestion
        }
        val addDataset = addDatasetAction { input: (File, String) =>
            val (upfile, uri) = input
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addDataset
            val tryschema = dm.getSchemaFromUri(uriCatalogManager, uri)

            val res: Try[Boolean] = tryschema.map(s =>  ingestionManager.write(s))

            val httpres= res match {
                case Success(true) => Successfull(Some("Dataset stored"))
                case Success(false) => Successfull(Some("ERROR dataset cannot be stored"))
                case Failure(ex) => Successfull(Some(s"ERROR dataset cannot be stored due: ${ex.getMessage}"))
            }
            AddDataset200(httpres)
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addDataset
        }
    
    }
}
