package controllers.ckan

/**
 * Created by ale on 04/04/17.
 */

import javax.inject._

import it.gov.daf.catalogmanager.Utils
import it.gov.daf.play.security.protocol._
import it.gov.daf.play.security.{CookieCache, ExternalServiceProxy, SingleSignOnClient}
import play.api.inject.ConfigurationProvider
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class CkanController @Inject() (ws: WSClient, config: ConfigurationProvider) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val ckanUrl: String = config.get.getString("app.ckan.url").get

  private val localUrl: String = config.get.getString("app.local.url").get

  private val userIdHeader: String = config.get.getString("app.userid.header").get

  private val securityManagerHost: String = config.get.getString("security.manager.host").get

  private val externalServiceProxy = new ExternalServiceProxy(
    ws = ws,
    //FIXME should it be a configuration
    cache = CookieCache(24.hours),
    //Change the thread pool if needed
    ssoClient = SingleSignOnClient(securityManagerHost)(ws, play.api.libs.concurrent.Execution.defaultContext)
  )

  private def getOrgs(orgId: String): Future[List[String]] = {
    val orgs: Future[WSResponse] = ws.url(localUrl + "/ckan/organizations/" + orgId).get()
    orgs.map(item => {
      val messages = (item.json \ "result" \\ "message").map(_.as[String]).toList
      val datasetIds: List[String] = messages.filterNot(_.isEmpty) map { message =>
        println(message)
        val datasetId = message.split("object")(1).trim
        println(datasetId)
        datasetId
      }
      datasetIds
    })
  }

  private def getOrgDatasets(datasetIds: List[String]): Future[Seq[JsValue]] = {
    val datasetResponses: Seq[Future[JsValue]] = datasetIds.map(datasetId => {
      val response = ws.url(localUrl + "/ckan/dataset/" + datasetId).get
      println(datasetId)
      response map { x =>
        println(x.json.toString)
        (x.json \ "result").getOrElse(JsNull)
      }
    })
    val datasetFuture: Future[Seq[JsValue]] = Future.sequence(datasetResponses)
    datasetFuture
  }

  def getOrganizationDataset(organizationId: String) = Action.async { implicit request =>
    def isNull(v: JsValue) = v match {
      case JsNull => true
      case _ => false
    }
    for {
      datasets <- getOrgs(organizationId)
      dataset: Seq[JsValue] <- getOrgDatasets(datasets)
    } yield {
      Ok(Json.obj("result" -> Json.toJson(dataset.filterNot(isNull(_))), "success" -> JsBoolean(true)))
    }
  }

  private implicit def cookieString(cookie: Cookie): String =
    s"${cookie.name}=${cookie.value}"

  private def parseRequest(request: Request[AnyContent]): Option[(JsValue, CkanCredentials)] =
    request.body.asJson.zip(request.headers.get(userIdHeader))
      .headOption
      .map { case (json, user) => json -> CkanCredentials(user) }

  private def parseResponse(response: Option[Future[WSResponse]]): Future[Result] =
    response match {
      case None => Future.successful(InternalServerError("Invalid Body or missing Cookie"))
      case Some(resp) => resp.map(r => Ok(r.body))
    }

  def createDataset = Action.async { implicit request =>
    //curl -H "Content-Type: application/json" -X POST -d @data.json http://localhost:9001/ckan/createDataset
    //val AUTH_TOKEN:String = config.get.getString("app.ckan.auth.token").get
    val response = parseRequest(request)
      .map {
        case (json, user) =>
          externalServiceProxy.callService(user) { cookie =>
            ws.url(s"$ckanUrl/api/3/action/package_create")
              //check https://stackoverflow.com/questions/18101324/play-framework-ws-set-cookie
              .withHeaders("Cookie" -> cookie).post(json)
          }
      }
    parseResponse(response)
  }

  def updateDataset(datasetId: String) = Action.async { implicit request =>
    //curl -H "Content-Type: application/json" -X PUT -d @datavv.json http://localhost:9001/ckan/updateDataset/id=81b643bd-007e-44cb-b724-0a02018db6d9
    val response = parseRequest(request).map {
      case (json, user) =>
        externalServiceProxy.callService(user) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/package_update?id=$datasetId")
            .withHeaders("Cookie" -> cookie).post(json)
        }
    }
    parseResponse(response)
  }

  def deleteDataset(datasetId: String) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        val b = Json.obj("id" -> datasetId)
        val body = s"""{\"id\":\"$datasetId\"}"""
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/package_delete")
            .withHeaders("Cookie" -> cookie).post(body)
        }
      }
    parseResponse(response)
  }

  def purgeDataset(datasetId: String) = Action.async { implicit request =>

    //curl -H "Content-Type: application/json" -X DELETE http://localhost:9001/ckan/purgeDataset/mydataset
    val response = request.headers.get(userIdHeader)
      .map { user =>
        val b = Json.obj("id" -> datasetId)
        val body = s"""{\"id\":\"$datasetId\"}"""
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/dataset_purge")
            .withHeaders("Cookie" -> cookie).post(body)
        }
      }
    parseResponse(response)
  }

  def createOrganization = Action.async { implicit request =>
    //curl -H "Content-Type: application/json" -X POST -d @org.json http://localhost:9001/ckan/createOrganization dove org.json contiene
    //val AUTH_TOKEN:String = config.get.getString("app.ckan.auth.token").get
    val response = parseRequest(request)
      .map {
        case (json, user) =>
          externalServiceProxy.callService(user) { cookie =>
            ws.url(s"$ckanUrl/api/3/action/organization_create")
              .withHeaders("Cookie" -> cookie).post(json)
          }
      }
    parseResponse(response)
  }

  def createUser = Action.async { implicit request =>

    /*
    settare la proprietà ckan.auth.create_user_via_api = true

    curl -H "Content-Type: application/json" -X POST -d @user.json http://localhost:9001/ckan/createUser dove user.json contiene
    {
      "name": "test_user",
      "email": "test@test.org",
      "password": "password",
      "fullname": "utente di test",
      "about": "prova inserimento utente di test"
    }
    * */

    val response = parseRequest(request)
      .map {
        case (json, user) =>
          externalServiceProxy.callService(user) { cookie =>
            ws.url(s"$ckanUrl/api/3/action/user_create")
              .withHeaders("Cookie" -> cookie).post(json)
          }
      }
    parseResponse(response)
  }

  def getUser(userId: String) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/user_show?id=$userId")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def getUserOrganizations(userId: String, permission: Option[String]) = Action.async { implicit request =>

    //CKAN versione 2.6.2 non prende l'id utente passato. Per la ricerca prende l'utente corrispondente all'API key
    //TODO aggiornare quindi l'interfaccia del servizio o la versione CKAN

    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/organization_list_for_user?id=$userId")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def getOrganization(orgId: String) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/organization_show?id=$orgId")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def updateOrganization(orgId: String) = Action.async { implicit request =>
    // curl -H "Content-Type: application/json" -X PUT -d @org.json http://localhost:9001/ckan/updateOrganization/id=232cad97-ecf2-447d-9656-63899023887t
    val response = parseRequest(request)
      .map {
        case (json, user) =>
          externalServiceProxy.callService(user) { cookie =>
            ws.url(s"$ckanUrl/api/3/action/organization_update?id=$orgId")
              .withHeaders("Cookie" -> cookie).post(json)
          }
      }
    parseResponse(response)
  }

  def patchOrganization(orgId: String) = Action.async { implicit request =>
    // curl -H "Content-Type: application/json" -X PUT -d @org.json http://localhost:9001/ckan/updateOrganization/id=232cad97-ecf2-447d-9656-63899023887t
    val response = parseRequest(request)
      .map {
        case (json, user) =>
          externalServiceProxy.callService(user) { cookie =>
            ws.url(s"$ckanUrl/api/3/action/organization_patch?id=$orgId")
              .withHeaders("Cookie" -> cookie).post(json)
          }
      }
    parseResponse(response)
  }

  def deleteOrganization(orgId: String) = Action.async { implicit request =>
    //curl -H "Content-Type: application/json" -X DELETE http://localhost:9001/ckan/deleteOrganization/apt-altopiano-di-pine-e-valle-di-cembra2
    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          val body = s"""{\"id\":\"$orgId\"}"""
          ws.url(s"$ckanUrl/api/3/action/organization_delete")
            .withHeaders("Cookie" -> cookie).post(body)
        }
      }
    parseResponse(response)
  }

  def purgeOrganization(orgId: String) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          val body = s"""{\"id\":\"$orgId\"}"""
          ws.url(s"$ckanUrl/api/3/action/organization_purge")
            .withHeaders("Cookie" -> cookie).post(body)
        }
      }
    parseResponse(response)
  }

  def getDatasetList = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/package_list")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def getDatasetListWithResources(limit: Option[Int], offset: Option[Int]) = Action.async { implicit request =>
    // curl -X GET "http://localhost:9001/ckan/datasetsWithResources?limit=1&offset=1"
    val response = request.headers.get(userIdHeader)
      .map { user =>
        val params = Map(("limit", limit), ("offset", offset))
        val queryString = Utils.buildEncodedQueryString(params)

        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/current_package_list_with_resources$queryString")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def getOrganizationList = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>

        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/organization_list")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def searchDataset(q: Option[String], sort: Option[String], rows: Option[Int], start: Option[Int]) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        val params = Map(("q", q), ("sort", sort), ("rows", rows), ("start", start))
        val queryString = Utils.buildEncodedQueryString(params)

        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/package_search$queryString")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def autocompleteDataset(q: Option[String], limit: Option[Int]) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        val params = Map(("q", q), ("limit", limit))
        val queryString = Utils.buildEncodedQueryString(params)

        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/package_autocomplete$queryString")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def getOganizationRevisionList(organizationId: String) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/organization_revision_list?id=$organizationId")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

  def getDataset(datasetId: String) = Action.async { implicit request =>
    val response = request.headers.get(userIdHeader)
      .map { user =>
        externalServiceProxy.callService(CkanCredentials(user)) { cookie =>
          ws.url(s"$ckanUrl/api/3/action/package_show?id=$datasetId")
            .withHeaders("Cookie" -> cookie).get
        }
      }
    parseResponse(response)
  }

}