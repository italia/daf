
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

import it.gov.daf.ingestionmanager.ClientCaller
import it.gov.daf.catalogmanager.StdUris
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.catalogmanager.MetaCatalog
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global

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
        val testingestion = testingestionAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.testingestion
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.testingestion
        }
        val addDataset = addDatasetAction { input: (File, String) =>
            val (upfile, uri) = input
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addDataset
            val res = ClientCaller.callCatalogManager(uri,upfile)
            AddDataset200(res)
            //AddDataset200(Successfull(Some("Dataset stored")))
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addDataset
        }
        val addNewDataset = addNewDatasetAction { (uri: String) =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
            import scala.concurrent.ExecutionContext.Implicits.global
            val auth = currentRequest.headers.get("authorization")
            println(auth)
            val res :Future[String]= ClientCaller.callCatalogManager(auth.getOrElse(""), uri)
            res onFailure {
                case t => println("An error has occured: " + t.getMessage)
            }
            AddNewDataset200(res)
            //res.map( x => {
            //     println(x.dcatapit.get.license_title.get)
            //   }
            //)
            //NotImplementedYet
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
        }
    
    }
}
