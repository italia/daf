
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
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.Future
import com.typesafe.config.Config
import it.gov.daf.ingestion.nifi.NifiProcessor
import it.gov.daf.ingestion.nifi.NifiProcessor.NifiResult
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package ingestion_manager.yaml {
    // ----- Start of unmanaged code area for package Ingestion_managerYaml
        
    // ----- End of unmanaged code area for package Ingestion_managerYaml
    class Ingestion_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Ingestion_managerYaml
    implicit
    val system: ActorSystem,
    implicit val ec: ExecutionContext,
    implicit val ws: WSClient,
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
      implicit val config: Config = com.typesafe.config.ConfigFactory.load()
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      val clientCaller = new ClientCaller(ws)

      val auth = currentRequest
        .headers
        .get("authorization")
        .getOrElse("")

      val fResults =
        clientCaller.clientCatalogMgrMetaCatalog(auth, ds_logical_uri)
          .flatMap(mc => NifiProcessor(mc).createDataFlow())

      fResults
        .flatMap(r =>
          AddNewDataset200(IngestionReport("200", Some(r.toString))))
        .recoverWith {
          case ex: Throwable =>
            ex.printStackTrace()
            AddNewDataset200(IngestionReport("400", Some(ex.getLocalizedMessage)))
        }
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
        }
    
    }
}
