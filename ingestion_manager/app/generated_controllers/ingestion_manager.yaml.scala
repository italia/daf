
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.ingestion.ClientCaller
import it.gov.daf.ingestion.nifi.NiFiBuilder
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.Future
import it.gov.daf.ingestion.nifi.NiFiProcessStatus

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
            //FIXME take out these resources and close them
      implicit val system: ActorSystem = ActorSystem()
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      val wsClient: AhcWSClient = AhcWSClient()
      val clientCaller = new ClientCaller(wsClient)
      implicit val ec = system.dispatcher

      val auth = currentRequest
        .headers
        .get("authorization")
        .getOrElse("")

      val fResult: Future[NiFiProcessStatus] =
        clientCaller.clientCatalogMgrMetaCatalog(auth, ds_logical_uri)
          .map { metaCatalog =>
            val niFiBuilder = new NiFiBuilder(wsClient)
            niFiBuilder.createProcessors(metaCatalog)
          }

      fResult
        .flatMap(r => AddNewDataset200(IngestionReport("Status: OK", Some(r.toString))))
        .recoverWith {
          case ex: Throwable =>
            ex.printStackTrace()
            AddNewDataset200(IngestionReport(s"Status: ${ex.getLocalizedMessage}", Some("NiFi Info")))
        }
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
        }
    
    }
}
