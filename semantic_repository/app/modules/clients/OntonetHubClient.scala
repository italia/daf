package modules.clients

import it.almawave.linkeddata.kb.utils.JSONHelper
import play.api.libs.ws.WSClient
import scala.concurrent.Future
import java.nio.file.Paths
import akka.stream.scaladsl.Source
import play.api.mvc.MultipartFormData.DataPart
import play.api.mvc.MultipartFormData.FilePart
import akka.stream.scaladsl.FileIO
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.concurrent.Await
import it.almawave.linkeddata.kb.utils.TryHandlers
import it.almawave.linkeddata.kb.utils.TryHandlers._
import scala.concurrent.Awaitable
import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.Float

object OntonethubClient {

  def create(implicit ws: WSClient) = new OntonethubClient(ws)

}

/**
 * This is a Client for wrapping OntonetHub component (based on Stanbol).
 * The idea behind it is to expose simple API for simpler interaction and integration
 * of OntonetHub/Stanbol with the other components handling semantics (triplestores...)
 *
 * TODO: refactorization, after local testing
 */
class OntonethubClient(ws: WSClient) {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._
  import scala.concurrent.ExecutionContext.Implicits._

  // TODO: export the configurations
  val host = "localhost"
  val port = 8000
  val FOLLOW_REDIRECTS = true
  val PAUSE = 1000

  def status(): Future[Boolean] = {
    ws.url(urls.status)
      .withFollowRedirects(FOLLOW_REDIRECTS)
      .get()
      .map { response =>
        (response.status == 200) && response.body.contains("OntoNetHub")
      }
  }

  // this object encapsulates various types of lookup
  object lookup {

    // returns a list of ontology uris from ontonethub
    def list_ids: Future[List[String]] = {
      list_uris.map {
        list =>
          list.map { node =>
            val uri = node
            uri.substring(uri.lastIndexOf("/") + 1)
          }
      }
    }

    /**
     * returns a list of ontology ids
     */
    def list_uris: Future[List[String]] = {
      ws.url(urls.ontologies_list)
        .withFollowRedirects(FOLLOW_REDIRECTS)
        .get()
        .map { response =>
          JSONHelper.read(response.body).toList.map(_.asText())
        }
    }

    /**
     * returns the id for the given prefix
     */
    def find_id_by_prefix(prefix: String): Future[String] = {
      ids_for_prefixes.map { map => map.get(prefix).get }
    }

    /**
     * returns a list of (prefix, id) pair
     */
    private def ids_for_prefixes() = {

      list_ids.flatMap { ids_list =>

        // for each ontology: retrieves the name associated with id
        val temp = ids_list.map { onto_id =>
          ws.url(urls.ontology_metadata(onto_id))
            .withFollowRedirects(FOLLOW_REDIRECTS)
            .get()
            .map { res => JSONHelper.read(res.body) }
            .map { json => (json.get("name").asText(), json.get("id").asText()) }
        }

        Future.sequence(temp).map(_.toMap)

      }

    }

    /**
     * checking the current status of the job after crud operations
     */
    def status(ontologyID: String): Future[String] = {
      ws.url(urls.job_status(ontologyID))
        .withFollowRedirects(FOLLOW_REDIRECTS)
        .get()
        .flatMap { response =>
          response.status match {
            case 200 =>
              val json = JSONHelper.read(response.body)
              json.get("status").textValue() match {
                // once an action has beeen requested, we have to check its status inside the JSON object
                case "finished" => Future.successful(s"ontology ${ontologyID} has been correctly uploaded")
                case "aborted"  => Future.failed(new Exception(response.body))
                case "running" =>
                  // we introduce a pause to avoid too much requests
                  Thread.sleep(PAUSE)
                  status(ontologyID)
              }
            // if the resource id does not exists yet, we have to handle anHTTP error 
            case 404 => Future.failed(new ResourceNotExistsException(s"the ${ontologyID} resource does not exists!"))
            case _   => Future.failed(new Exception(response.body))
          }
        }
    }

  }

  // this object encapsulates the methods for CRUD operations
  object crud {

    def find(query: String, limit: Int = 10, lang: String = "") = {

      // CHECK: Play.application().configuration().getString("sms.service.url"
      // DISABLED: OntonethubFindParser.parse(response.body)

      ws.url(urls.find())
        .withFollowRedirects(FOLLOW_REDIRECTS)
        .withHeaders(("accept", "application/json"))
        .withHeaders(("content-type", "application/x-www-form-urlencoded"))
        .post(s"name=${query}&limit=${limit}&lang=${lang}")
        .map { response =>
          response.status match {
            case 200 => OntonethubFindParser.parse(response.body)
            case _   => List()
          }
        }

    }

    // retrieves the content (source) of an ontology
    def get(ontologyID: String, mime: String = "text/turtle"): Future[String] = {

      ws.url(urls.ontology_source(ontologyID))
        .withFollowRedirects(FOLLOW_REDIRECTS)
        .withHeaders(("Accept", "text/turtle"))
        .get()
        .map { res => res.body }

    }

    // deletes all the loaded ontologies
    def clear() = {
      val results = lookup.list_ids.map { ids => ids.map { id => crud.delete_by_id(id) } }
      Await.result(results, Duration.Inf)
    }

    // adds an ontology
    def add(filePath: String, fileName: String, fileMime: String,
            description: String,
            prefix: String, uri: String): Future[String] = {

      val rdf_file = Paths.get(filePath).toAbsolutePath()
      val add_src = Source(List(
        DataPart("name", prefix),
        DataPart("description", description),
        DataPart("baseUri", uri),
        FilePart("data", fileName, Option(fileMime), FileIO.fromPath(rdf_file))))

      val res = ws.url(urls.add_ontology)
        .withHeaders(("Accept", "application/json"))
        .withFollowRedirects(FOLLOW_REDIRECTS)
        .post(add_src)

      // preparing results, with a minimal error handling
      res.flatMap { response =>
        response.status match {
          case 200 =>
            val json = JSONHelper.read(response.body)
            val _id: String = json.get("ontologyId").textValue()
            for {
              ok <- Future.successful(_id)
              status <- lookup.status(_id)
            } yield ok
          case 409 => Future.failed(new ResourceAlreadyExistsException(s"the ${prefix} resource already exists!"))
          case _   => Future.failed(new Exception(response.body))
        }
      }

    }

    // deletes an ontology, by id
    def delete_by_id(ontologyID: String): Future[String] = {

      ws.url(urls.delete_ontology(ontologyID))
        .withFollowRedirects(FOLLOW_REDIRECTS)
        .delete()
        .flatMap { res =>
          res.status match {
            case 200 =>
              for {
                ok <- Future.successful(s"ontology with id ${ontologyID} deleted")
                status <- lookup.status(ontologyID)
              } yield ontologyID

            case 404 => Future.failed(new ResourceNotExistsException)
            case _   => Future.failed(new Exception(res.body))
          }
        }

    }

  }

  object urls {

    def job_status(ontologyID: String) = s"http://${host}:${port}/stanbol//jobs/${ontologyID}"

    // list of all the ontologies
    def ontologies_list = s"http://${host}:${port}/stanbol/ontonethub/ontologies"

    // find lookup url
    def find() = s"http://${host}:${port}/stanbol/ontonethub/ontologies/find"

    // search on ontology, by ontology id
    def find_by_id(ontologyID: String) = s"http://${host}:${port}/stanbol/ontonethub/ontology/${ontologyID}/find"

    // ontology metadata
    def ontology_metadata(ontologyID: String) = s"http://${host}:${port}/stanbol/ontonethub/ontology/${ontologyID}"

    // ontology source (in JSON)
    def ontology_source(ontologyID: String) = s"http://${host}:${port}/stanbol/ontonethub/ontology/${ontologyID}/source"

    def add_ontology() = s"http://${host}:${port}/stanbol/ontonethub/ontology"

    def delete_ontology(onto_id: String) = s"http://${host}:${port}/stanbol/ontonethub/ontology/${onto_id}"

    def status = s"http://${host}:${port}/stanbol/ontonethub/"

  }

}

class ResourceAlreadyExistsException(msg: String = "the resource already exists!") extends RuntimeException(msg)

class ResourceNotExistsException(msg: String = "the resource does not exists!") extends RuntimeException(msg)
