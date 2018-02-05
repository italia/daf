
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
import com.typesafe.config.Config
import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.ingestion.ClientCaller
import it.gov.daf.ingestion.nifi.NifiProcessor
import it.gov.daf.ingestion.pipelines.PipelineInfoRead
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext
import java.net.URLEncoder
import it.gov.daf.securitymanager.client.Security_managerClient
import it.gov.daf.common.utils.DafUriConverter

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package ingestion_manager.yaml {

  import it.gov.daf.common.utils.{Ordinary, Standard}
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

        // ----- End of unmanaged code area for constructor Ingestion_managerYaml
        val addNewDataset = addNewDatasetAction { (ds_logical_uri: String) =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.addNewDataset
            //FIXME take out these resources and close them
      implicit val config: Config = com.typesafe.config.ConfigFactory.load()
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      val clientCaller = new ClientCaller(ws)

      val securityClient = new Security_managerClient(ws)(
        baseUrl = config.getString("daf.security_manager_url")
      )


      val auth = currentRequest
        .headers
        .get("authorization")
        .getOrElse("")

      val fResults = clientCaller.clientCatalogMgrMetaCatalog(auth, ds_logical_uri)
          .flatMap{ mc =>
            //TODO move this logic into common
            val user = mc.operational.group_own
            val domain = mc.operational.theme
            val subDomain = mc.operational.subtheme
            val dsName = mc.dcatapit.name


            val path =  URLEncoder.encode(s"/home/$user/$domain/$subDomain/$dsName", "UTF-8")

            //securityClient.sftp(auth,mc.operational.group_own, path) //TODO there is one too much of an argument
            securityClient.sftp(auth, path)
              .map(res => mc)
          }
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
        val pipelineListAll = pipelineListAllAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.pipelineListAll
            PipelineListAll200(PipelineInfoRead.pipelineInfo())
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.pipelineListAll
        }
        val pipelineListByCat = pipelineListByCatAction { (pipeline_category: String) =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.pipelineListByCat
            PipelineListByCat200(PipelineInfoRead.pipelineInfoByCat(pipeline_category))
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.pipelineListByCat
        }
        val pipelineListById = pipelineListByIdAction { (pipeline_id: String) =>  
            // ----- Start of unmanaged code area for action  Ingestion_managerYaml.pipelineListById
            PipelineListById200(PipelineInfoRead.pipelineInfoById(pipeline_id))
            // ----- End of unmanaged code area for action  Ingestion_managerYaml.pipelineListById
        }
    
    }
}
