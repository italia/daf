
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import it.gov.daf.ingestion.ClientCaller
import it.gov.daf.ingestion.utilities.WebServiceUtil
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.catalogmanager.MetaCatalog
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.ingestion.metacatalog.MetaCatalogProcessor
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.Future
import scala.util.{Failure,Success}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.ingestion.nifi.NiFiBuilder
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
        NotImplementedYet
        // ----- End of unmanaged code area for constructor Ingestion_managerYaml
        val testmicrosrv = testmicrosrvAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.testmicrosrv
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.testmicrosrv
        }
        val addNewDataset = addNewDatasetAction { (ds_logical_uri: String) =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
            import scala.concurrent.ExecutionContext.Implicits.global
          implicit val system: ActorSystem = ActorSystem()
          implicit val materializer: ActorMaterializer = ActorMaterializer()
            val auth = currentRequest.headers.get("authorization")
            val resFuture :Future[MetaCatalog]= ClientCaller.clientCatalogMgrMetaCatalog(auth.getOrElse(""), ds_logical_uri)

            val result: Try[MetaCatalog] = Await.ready(resFuture, Duration.Inf).value.get

            result match {
              case Success(metaCatalog) =>
                val client: AhcWSClient = AhcWSClient()
                val niFiBuilder = new NiFiBuilder(client, metaCatalog)
                //val niFiBuilder = new NiFiBuilder(client)

                val niFiResults = niFiBuilder.processorBuilder()
                AddNewDataset200(IngestionReport("Status: OK", Some(niFiResults.toString)))

              case Failure(e) =>
                println(e.printStackTrace())
                AddNewDataset200(IngestionReport("Status: Error", Some("NiFi Info")))

            }
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
        }
    
    }
}
