
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
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.ingestionmanager.IngestionManager
import play.api.libs.ws.ahc.{AhcWSClient,AhcWSRequest}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.ingestionmanager.IngestionManager
import play.api.http.Writeable
import play.api.libs.ws.ahc.{AhcWSClient,AhcWSRequest}
import scala.concurrent.{Await,Future}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.ingestionmanager.IngestionManager
import play.api.http.Writeable
import play.api.libs.ws.ahc.{AhcWSClient,AhcWSRequest}
import scala.concurrent.{Await,Future}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.ingestionmanager.IngestionManager
import play.api.http.Writeable
import play.api.libs.ws.ahc.{AhcWSClient,AhcWSRequest}
import scala.concurrent.{Await,Future}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.ingestionmanager.IngestionManager
import play.api.http.Writeable
import play.api.libs.ws.ahc.{AhcWSClient,AhcWSRequest}
import scala.concurrent.{Await,Future}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import scala.concurrent.ExecutionContext.Implicits.global
import it.gov.daf.ingestionmanager.IngestionManager
import play.api.http.Writeable
import play.api.libs.ws.ahc.{AhcWSClient,AhcWSRequest}
import scala.concurrent.{Await,Future}
import scala.concurrent.duration.Duration

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package ingestion_manager.yaml {
    // ----- Start of unmanaged code area for package Ingestion_managerYaml
                            import akka.actor.ActorSystem
                    import akka.stream.ActorMaterializer
                    import it.gov.daf.catalogmanager.client.Catalog_managerClient

                    import scala.concurrent.ExecutionContext.Implicits.global
                    import it.gov.daf.ingestionmanager.IngestionManager
                    import play.api.http.Writeable
                    import play.api.libs.ws.ahc.{AhcWSClient, AhcWSRequest}

                    import scala.concurrent.{Await, Future}
                    import scala.concurrent.duration.Duration
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


//        val invoker = new ApiInvoker()
//        //val client = new JWTTokenApi(defBasePath = s"http://localhost:$port/security-manager/v1", defApiInvoker = invoker)
//        val catalogManagerApi: DatasetCatalogApi = new DatasetCatalogApi(defApiInvoker = invoker)




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
            implicit val system: ActorSystem = ActorSystem()
            implicit val materializer: ActorMaterializer = ActorMaterializer()
            val client: AhcWSClient = AhcWSClient()
            val catalogManager = new Catalog_managerClient(client)(uriCatalogManager)
            //val service = s"$uriCatalogManager/dataset-catalogs/$uri"
            //val response = ingestionManager.connect(client)(service)

          val response = catalogManager.datasetcatalogbyid("",uri)
            val res = response
              .map(s =>  ingestionManager.write(s, upfile))
              .map{
                  case Success(true) => Successfull(Some("Dataset stored"))
                  case Success(false) => Successfull(Some("ERROR dataset cannot be stored"))
                  case Failure(ex) => Successfull(Some(s"ERROR ${ex.getMessage}"))
              }
             // .map( AddDataset200(_))

            client.close()
            val r = Await.result(res, Duration.Inf)
            println(r)
            AddDataset200(r)
            //AddDataset200(Successfull(Some("Dataset stored")))
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.addDataset
        }
    
    }
}
