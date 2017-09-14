
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
import play.api.mvc.{Action,Controller}
import play.api.data.validation.Constraint
import play.api.i18n.MessagesApi
import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}
import de.zalando.play.controllers._
import PlayBodyParsing._
import PlayValidations._
import modules.KBModuleBase
import play.api.libs.ws.WSClient
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import it.almawave.linkeddata.kb.utils.TryHandlers._
import java.net.URLDecoder
import java.net.URI
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package semantic_repository.yaml {
    // ----- Start of unmanaged code area for package Semantic_repositoryYaml
                                                                                        
    // ----- End of unmanaged code area for package Semantic_repositoryYaml
    class Semantic_repositoryYaml @Inject() (
        // ----- Start of unmanaged code area for injections Semantic_repositoryYaml

      kb: KBModuleBase,
      ws: WSClient,

        // ----- End of unmanaged code area for injections Semantic_repositoryYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Semantic_repositoryYamlBase {
        // ----- Start of unmanaged code area for constructor Semantic_repositoryYaml

    // wrapper for triplestore
    val kbrepo = kb.kbrepo

        // ----- End of unmanaged code area for constructor Semantic_repositoryYaml
        val countTriplesByOntology = countTriplesByOntologyAction { (prefix: String) =>  
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.countTriplesByOntology
            val namespace = kbrepo.prefixes.list().get(prefix)

      kbrepo.store.size(namespace) match {
        case Success(triples) =>
          CountTriplesByOntology200(Future {
            TriplesCount(prefix, triples)
          })
        case Failure(err) =>
          val err_msg = s"cannot count triples for prefix ${prefix}"
          CountTriplesByOntology400(Future {
            Error(err_msg, s"${err}")
          })
      }
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.countTriplesByOntology
        }
        val prefixesList = prefixesListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.prefixesList
            val prefixes = kbrepo.prefixes.list().get
        .toList
        .map(item => Prefix(item._1, item._2))

      PrefixesList200(Future {
        prefixes
      })
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.prefixesList
        }
        val removeRDFDoc = removeRDFDocAction { (context: String) =>  
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.removeRDFDoc
            // REVIEW the parameters
      Try { kbrepo.store.clear(context) } match {

        case Success(_) =>
          val msg = s"""all the triples in the context ${context} were deleted correctly"""
          RemoveRDFDoc200(Future {
            ResultMessage(msg)
          })

        case Failure(err) =>
          val msg = s"""the triples in the context ${context} cannot be removed"""
          RemoveRDFDoc400(Future { Error(msg, s"${err}") })

      }
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.removeRDFDoc
        }
        val prefixDirectLookup = prefixDirectLookupAction { (prefix: String) =>  
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.prefixDirectLookup
            lazy val prefixes = kbrepo.prefixes.list()
      val _namespace = prefixes.get(prefix)
      PrefixDirectLookup200(Future {
        Prefix(prefix, _namespace)
      })
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.prefixDirectLookup
        }
        val countTriples = countTriplesAction {  _ =>  
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.countTriples
            val triples = kbrepo.store.size()
      CountTriples200(Future {
        TriplesCount("_ALL_", triples.get)
      })
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.countTriples
        }
        val addRDFDoc = addRDFDocAction { input: (String, File, String, String) =>
            val (fileName, rdfDocument, prefix, context) = input
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.addRDFDoc
            val _context = URLDecoder.decode(context, "UTF-8")

      Try {
        val context_uri = URI.create(_context) // check problems parsing URI for context
        kbrepo.io.addFile(fileName, rdfDocument, prefix, context_uri.toString())
        //        kbrepo.io.addFile("testing_foaf_00.rdf", rdfDocument, prefix, context_uri.toString())

      } match {
        case Success(_) =>
          val msg = s"""the document ${fileName} was correctly added to context ${prefix}:<${_context}>"""
          AddRDFDoc200(Future {
            ResultMessage(msg)
          })
        case Failure(err) =>
          val msg = s"""the document ${rdfDocument.getAbsoluteFile} cannot be added to context ${prefix}:<${_context}>"""

          AddRDFDoc500(Future { Error(msg, s"${err}") })
      }
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.addRDFDoc
        }
        val prefixReverseLookup = prefixReverseLookupAction { (namespace: String) =>  
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.prefixReverseLookup
            val prefixes = kbrepo.prefixes.list().get.map(item => (item._2, item._1))
      val _prefix = prefixes.get(namespace).get
      PrefixReverseLookup200(Future {
        Prefix(_prefix, namespace)
      }) // FIX encode/decode!
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.prefixReverseLookup
        }
        val contextsList = contextsListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Semantic_repositoryYaml.contextsList
            val _contexts = kbrepo.store.contexts().get
        .map { cx =>
          //          Context(cx, kbrepo.store.size(vf.createIRI(cx)).get)
          Context(cx, kbrepo.store.size(cx).get)
        }
      ContextsList200(Future {
        _contexts
      })
            // ----- End of unmanaged code area for action  Semantic_repositoryYaml.contextsList
        }
    
    }
}
