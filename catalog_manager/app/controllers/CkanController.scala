package controllers.ckan

/**
  * Created by ale on 04/04/17.
  */


import javax.inject._

import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.Future
import play.api.libs.json._
import play.api.inject.ConfigurationProvider
import it.gov.daf.catalogmanager.service.CkanRegistry
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.common.utils.WebServiceUtil
import play.api.Logger
//import play.api.libs.ws.ahc.AhcWSClient



@Singleton
class CkanController @Inject() (wsc: WSClient, config: ConfigurationProvider, secInvokManager:SecuredInvocationManager) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val CKAN_URL :String = config.get.getString("app.ckan.url").get

  private val LOCAL_URL :String = config.get.getString("app.local.url").get

  private val ENV:String = config.get.getString("app.type").get

  private val USER_ID_HEADER:String = config.get.getString("app.userid.header").get

  private val SEC_MANAGER_HOST:String = config.get.getString("security.manager.host").get

  //private val secInvokManager = SecuredInvocationManager.init( LoginClientRemote.init(SEC_MANAGER_HOST) )

  private def getOrgs(orgId :String): Future[List[String]] = {
    val orgs : Future[WSResponse] = wsc.url(LOCAL_URL + "/ckan/organizations/" + orgId).get()
    orgs.map( item => {
      val messages  = (item.json \ "result" \\ "message").map(_.as[String]).toList
      val datasetIds :List[String]= messages.filterNot(_.isEmpty) map { message =>
        Logger.debug(message)
        val datasetId = message.split("object")(1).trim
        Logger.debug(datasetId)
        datasetId
      }
      datasetIds
    })
  }

  private def getOrgDatasets(datasetIds : List[String]): Future[Seq[JsValue]] = {
    val datasetResponses: Seq[Future[JsValue]] = datasetIds.map(datasetId => {
      val response = wsc.url(LOCAL_URL + "/ckan/dataset/" + datasetId).get
      Logger.debug(datasetId)
      response map { x =>
        Logger.debug(x.json.toString)
        (x.json \ "result").getOrElse(JsNull)
      }
    })
    val datasetFuture :Future[Seq[JsValue]] = Future.sequence(datasetResponses)
    datasetFuture
  }


  def getOrganizationDataset(organizationId :String) = Action.async { implicit request =>
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


  def createDataset = Action.async { implicit request =>


    //curl -H "Content-Type: application/json" -X POST -d @data.json http://localhost:9001/ckan/createDataset


    //val AUTH_TOKEN:String = config.get.getString("app.ckan.auth.token").get

    val json:JsValue = request.body.asJson.get

    if(ENV == "dev"){
      CkanRegistry.ckanService.createDataset(json,Option(""))

      val isOk = Future.successful(JsString("operazione effettuata correttamente"))
      isOk map { x =>
        Ok(x)
      }
    }else{

      val user = request.headers.get(USER_ID_HEADER).getOrElse("")

      val jsonString = json.toString().replace("privatex","private")// Temporary PATCH for swagger libs issues
      def callCreateDataset(cookie: String,wsClient: WSClient):Future[WSResponse] = {
        // wsClient.url(CKAN_URL + "/api/3/action/package_create").withHeaders("Cookie" -> cookie).post(jsonString)
       // TODO NO GOOD URL STRING PUT IN CONFIG
        wsClient.url("http://ckan-geo.default.svc.cluster.local:5000" + "/api/3/action/package_create").withHeaders("Cookie" -> cookie).post(jsonString)

      }

      secInvokManager.manageServiceCall(new LoginInfo(null,null,"ckan-geo"), callCreateDataset).map { r => Ok(r.body) }

    }
  }


  def updateDataset(datasetId :String)= Action.async { implicit request =>

    //curl -H "Content-Type: application/json" -X PUT -d @datavv.json http://localhost:9001/ckan/updateDataset/id=81b643bd-007e-44cb-b724-0a02018db6d9

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")
    val json:JsValue = request.body.asJson.get

    def callUpdateDataset(cookie: String, wsClient: WSClient):Future[WSResponse] ={
      wsClient.url(CKAN_URL + "/api/3/action/package_update?id=" + datasetId).withHeaders("Cookie" -> cookie).post(json)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callUpdateDataset)map { r => Ok(r.body) }

  }



  def deleteDataset(datasetId :String) = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callDeleteDataset( cookie: String, wsClient: WSClient ):Future[WSResponse] ={

      val url = CKAN_URL + "/api/3/action/package_delete"
      val body = s"""{\"id\":\"$datasetId\"}"""
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(body)

    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callDeleteDataset)map { r => Ok(r.body) }

  }



  def purgeDataset(datasetId :String) = Action.async { implicit request =>

    //curl -H "Content-Type: application/json" -X DELETE http://localhost:9001/ckan/purgeDataset/mydataset

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callPurgeDataset( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = CKAN_URL + "/api/3/action/dataset_purge"
      val body = s"""{\"id\":\"$datasetId\"}"""
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(body)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callPurgeDataset)map { r => Ok(r.body) }
  }


  def createOrganization = Action.async { implicit request =>


    //curl -H "Content-Type: application/json" -X POST -d @org.json http://localhost:9001/ckan/createOrganization dove org.json contiene

    //val AUTH_TOKEN:String = config.get.getString("app.ckan.auth.token").get

    val json:JsValue = request.body.asJson.get
    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callCreateOrganization( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      wsClient.url(CKAN_URL + "/api/3/action/organization_create").withHeaders("Cookie" -> cookie).post(json)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callCreateOrganization)map { r => Ok(r.body) }

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

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")
    val json:JsValue = request.body.asJson.get

    def callCreateUser( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      wsClient.url(CKAN_URL + "/api/3/action/user_create").withHeaders("Cookie" -> cookie).post(json)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callCreateUser)map { r => Ok(r.body) }

  }


  def getUser(userId :String) = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callGetUser( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = CKAN_URL + "/api/3/action/user_show?id=" + userId
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callGetUser)map { r => Ok(r.body) }

  }


  def getUserOrganizations( userId :String, permission:Option[String] ) = Action.async { implicit request =>

    //CKAN versione 2.6.2 non prende l'id utente passato. Per la ricerca prende l'utente corrispondente all'API key
    //TODO aggiornare quindi l'interfaccia del servizio o la versione CKAN

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callGetUserOrganizations( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = CKAN_URL + "/api/3/action/organization_list_for_user?id="+userId
      Logger.debug("organization_list_for_user URL " + url)
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callGetUserOrganizations)map { r => Ok(r.body) }

  }


  def getOrganization(orgId :String) = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callGetOrganization( cookie: String, wsClient: WSClient ):Future[WSResponse] ={
      val url = CKAN_URL + "/api/3/action/organization_show?id=" + orgId
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callGetOrganization)map { r => Ok(r.body) }

  }


  def updateOrganization(orgId :String)= Action.async { implicit request =>

    // curl -H "Content-Type: application/json" -X PUT -d @org.json http://localhost:9001/ckan/updateOrganization/id=232cad97-ecf2-447d-9656-63899023887t

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")
    val json:JsValue = request.body.asJson.get

    def callUpdateOrganization( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + "/api/3/action/organization_update?id=" + orgId
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(json)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callUpdateOrganization)map { r => Ok(r.body) }

  }

  def patchOrganization(orgId :String)= Action.async { implicit request =>

    // curl -H "Content-Type: application/json" -X PUT -d @org.json http://localhost:9001/ckan/updateOrganization/id=232cad97-ecf2-447d-9656-63899023887t

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")
    val json:JsValue = request.body.asJson.get

    def callPatchOrganization( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + "/api/3/action/organization_patch?id=" + orgId
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(json)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callPatchOrganization)map { r => Ok(r.body) }

  }


  def deleteOrganization(orgId :String) = Action.async { implicit request =>


    //curl -H "Content-Type: application/json" -X DELETE http://localhost:9001/ckan/deleteOrganization/apt-altopiano-di-pine-e-valle-di-cembra2

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callDeleteOrganization( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + "/api/3/action/organization_delete"
      val body = s"""{\"id\":\"$orgId\"}"""
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(body)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callDeleteOrganization)map { r => Ok(r.body) }
  }


  def purgeOrganization(orgId :String) = Action.async { implicit request =>

    //curl -H "Content-Type: application/json" -X DELETE http://localhost:9001/ckan/purgeOrganization/apt-altopiano-di-pine-e-valle-di-cembra2

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callPurgeOrganization( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + "/api/3/action/organization_purge"
      val body = s"""{\"id\":\"$orgId\"}"""
      wsClient.url(url).withHeaders("Cookie" -> cookie).post(body)
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callPurgeOrganization)map { r => Ok(r.body) }
  }


  def getDatasetList = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callGetDatasetList( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      wsClient.url(CKAN_URL + "/api/3/action/package_list").withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callGetDatasetList)map { r => Ok(r.body) }
  }


  def getDatasetListWithResources(limit:Option[Int], offset:Option[Int]) = Action.async { implicit request =>

    // curl -X GET "http://localhost:9001/ckan/datasetsWithResources?limit=1&offset=1"

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    val params= Map( ("limit",limit), ("offset",offset) )
    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    def callDatasetListWithResources( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + s"/api/3/action/current_package_list_with_resources$queryString"
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callDatasetListWithResources)map { r => Ok(r.body) }
  }


  def getOrganizationList = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callGetOrganizationList( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      wsClient.url(CKAN_URL + "/api/3/action/organization_list").withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callGetOrganizationList)map { r => Ok(r.body) }

  }


  def searchDataset(q:Option[String], sort:Option[String], rows:Option[Int], start:Option[Int]) = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    val params= Map( ("q",q), ("sort",sort), ("rows",rows), ("start",start), ("include_private","true") )
    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    def callSearchDataset( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + s"/api/3/action/package_search$queryString"
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callSearchDataset)map { r => Ok(r.body) }

  }

  def autocompleteDataset(q:Option[String], limit:Option[Int]) = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    val params= Map( ("q",q), ("limit",limit) )
    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    def autocompleteDataset( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + s"/api/3/action/package_autocomplete$queryString"
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), autocompleteDataset)map { r => Ok(r.body) }

  }


  def getOganizationRevisionList(organizationId :String) = Action.async { implicit request =>

    val user = request.headers.get(USER_ID_HEADER).getOrElse("")

    def callGetOganizationRevisionList( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
      val url = CKAN_URL + "/api/3/action/organization_revision_list?id=" + organizationId
      wsClient.url(url).withHeaders("Cookie" -> cookie).get
    }

    secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callGetOganizationRevisionList)map { r => Ok(r.body) }
  }


  def getDataset(datasetId :String) = Action.async { implicit request =>

    if(ENV == "dev"){
      val dataset = CkanRegistry.ckanService.dataset(datasetId,Option(""))

      val isOk = Future.successful(dataset)
      isOk map { x =>
        Ok(x)
      }

    }else{

      val user = request.headers.get(USER_ID_HEADER).getOrElse("")

      def callGetDataset( cookie: String, wsClient: WSClient ):Future[WSResponse] = {
        val url = CKAN_URL + "/api/3/action/package_show?id=" + datasetId
        wsClient.url(url).withHeaders("Cookie" -> cookie).get
      }

      secInvokManager.manageServiceCall(new LoginInfo(user,null,"ckan"), callGetDataset)map { r => Ok(r.body) }
    }

  }

}