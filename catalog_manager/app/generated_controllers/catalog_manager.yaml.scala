
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
import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.catalogmanager.listeners.IngestionListenerImpl
import it.gov.daf.catalogmanager.service.{CkanRegistry,ServiceRegistry}
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import it.gov.daf.common.authentication.Authentication
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.catalogmanager.service.VocServiceRegistry
import play.api.libs.ws.WSClient
import java.net.URLEncoder
import it.gov.daf.catalogmanager.utilities.ConfigReader
import play.api.libs.ws.WSResponse
import play.api.libs.ws.WSAuthScheme
import java.io.FileInputStream
import play.Environment
import it.gov.daf.catalogmanager.kylo.KyloTrasformers
import catalog_manager.yaml
import it.gov.daf.catalogmanager.kylo.Kylo
import it.gov.daf.common.sso.common.CredentialManager
import play.api.Logger
import it.gov.daf.catalogmanager

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package catalog_manager.yaml {
    // ----- Start of unmanaged code area for package Catalog_managerYaml
                                                                                                                                                                                                                                            
    // ----- End of unmanaged code area for package Catalog_managerYaml
    class Catalog_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Catalog_managerYaml

        ingestionListener : IngestionListenerImpl,
        val kylo :Kylo,
        val configuration: Configuration,
        val playSessionStore: PlaySessionStore,
        val ws: WSClient,
        // ----- End of unmanaged code area for injections Catalog_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Catalog_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Catalog_managerYaml

        val GENERIC_ERROR=Error("An Error occurred", None,None)
        Authentication(configuration, playSessionStore)
        val SEC_MANAGER_HOST = config.get.getString("security.manager.host").get
        val KYLOURL = config.get.getString("kylo.url").get
        val KYLOUSER = config.get.getString("kylo.user").getOrElse("dladmin")
        val KYLOPWD = config.get.getString("kylo.userpwd").getOrElse("XXXXXXXXXXX")

        // ----- End of unmanaged code area for constructor Catalog_managerYaml
        val autocompletedummy = autocompletedummyAction { (autocompRes: AutocompRes) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.autocompletedummy
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.autocompletedummy
        }
        val searchdataset = searchdatasetAction { input: (MetadataCat, MetadataCat, ResourceSize, ResourceSize) =>
            val (q, sort, rows, start) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.searchdataset
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            //if( ! CkanRegistry.ckanService.verifyCredentials(credentials) )
            //Searchdataset401(Error(None,Option("Invalid credentials!"),None))

            val datasetsFuture: Future[JsResult[Seq[Dataset]]] = CkanRegistry.ckanService.searchDatasets(input, Option(credentials.username))
            val eitherDatasets: Future[Either[String, Seq[Dataset]]] = datasetsFuture.map(result => {
                result match {
                    case s: JsSuccess[Seq[Dataset]] => Right(s.get)
                    case e: JsError => Left( WebServiceUtil.getMessageFromJsError(e) )
                }
            })
            // Getckandatasetbyid200(dataset)
            eitherDatasets.flatMap {
                case Right(dataset) => Searchdataset200(dataset)
                case Left(error) => Searchdataset401(Error(error,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.searchdataset
        }
        val getckanorganizationbyid = getckanorganizationbyidAction { (org_id: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckanorganizationbyid
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val orgFuture: Future[JsResult[Organization]] = CkanRegistry.ckanService.getOrganization(org_id, Option(credentials.username))
            val eitherOrg: Future[Either[String, Organization]] = orgFuture.map(result => {
                result match {
                    case s: JsSuccess[Organization] => Right(s.get)
                    case e: JsError => Left( WebServiceUtil.getMessageFromJsError(e) )
                }
            })

            eitherOrg.flatMap {
                case Right(organization) => Getckanorganizationbyid200(organization)
                case Left(error) => Getckanorganizationbyid401(Error(error,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.getckanorganizationbyid
        }
        val createdatasetcatalogExtOpenData = createdatasetcatalogExtOpenDataAction { (catalog: MetaCatalog) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.createdatasetcatalogExtOpenData
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            if( CredentialManager.isDafEditor(currentRequest) || CredentialManager.isDafAdmin(currentRequest) ) {
                val created: Success = ServiceRegistry.catalogService.createCatalogExtOpenData(catalog, Option(credentials.username), ws)
                CreatedatasetcatalogExtOpenData200(created)
            }else
                CreatedatasetcatalogExtOpenData401("Admin or editor permissions required")
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createdatasetcatalogExtOpenData
        }
        val getckandatasetList = getckandatasetListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckandatasetList
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val eitherOut: Future[Either[Error, Seq[String]]] = CkanRegistry.ckanService.getDatasets(Option(credentials.username)).map(result =>{
                result match {
                    case s: JsArray => Right(s.as[Seq[String]])
                    case _ => Left(GENERIC_ERROR)
                }
            })

            eitherOut.flatMap {
                case Right(list) => GetckandatasetList200(list)
                case Left(error) => GetckandatasetList401(error)
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.getckandatasetList
        }
        val voc_subthemesgetall = voc_subthemesgetallAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_subthemesgetall
            val subthemeList: Seq[VocKeyValueSubtheme] = VocServiceRegistry.vocRepository.listSubthemeAll()
            Voc_subthemesgetall200(subthemeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_subthemesgetall
        }
        val deleteCatalog = deleteCatalogAction { input: (String, String) =>
            val (name, org) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.deleteCatalog
            val user = CredentialManager.readCredentialFromRequest(currentRequest).username
            val feedName = s"${org}.${org}_o_${name}"
            val futureDeleteFromKylo: Future[Either[Error, Success]] = for{
                _ <- ServiceRegistry.catalogService.deleteCatalogByName(name, user)
                res <- kylo.deleteFeed(feedName, user).map(b => b)
            }yield res

            futureDeleteFromKylo.flatMap(res => if(res.isRight) DeleteCatalog200(res.right.get) else DeleteCatalog400(res.left.get))

//          NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.deleteCatalog
        }
        val datasetcatalogs = datasetcatalogsAction { input: (MetadataRequired, Dataset_catalogsGetLimit) =>
            val (page, limit) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.datasetcatalogs
            val pageIng :Option[Int] = page
            val limitIng :Option[Int] = limit
            val catalogs  = ServiceRegistry.catalogService.listCatalogs(page,limit)

            catalogs match {
                case Seq() => Datasetcatalogs401("No data")
                case _ => Datasetcatalogs200(catalogs)
            }
            // Datasetcatalogs200(catalogs)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.datasetcatalogs
        }
        val voc_subthemesgetbyid = voc_subthemesgetbyidAction { (themeid: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_subthemesgetbyid
            val subthemeList: Seq[KeyValue] = VocServiceRegistry.vocRepository.listSubtheme(themeid)
            Voc_subthemesgetbyid200(subthemeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_subthemesgetbyid
        }
        val voc_dcat2dafsubtheme = voc_dcat2dafsubthemeAction { input: (String, String) =>
            val (themeid, subthemeid) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcat2dafsubtheme
            val themeList: Seq[VocKeyValueSubtheme] = VocServiceRegistry.vocRepository.dcat2DafSubtheme(input._1, input._2)
            Voc_dcat2dafsubtheme200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcat2dafsubtheme
        }
        val standardsuri = standardsuriAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.standardsuri
            // Pagination wrong refactor login to db query
            val catalogs = ServiceRegistry.catalogService.listCatalogs(Some(1), Some(500))
            val uris: Seq[String] = catalogs.filter(x=> x.operational.is_std)
              .map(_.operational.logical_uri).map(_.toString)
            val stdUris: Seq[StdUris] = uris.map(x => StdUris(Some(x), Some(x)))
            Standardsuri200(Seq(StdUris(Some("ale"), Some("test"))))
            // NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.standardsuri
        }
        val datasetcatalogbyname = datasetcatalogbynameAction { (name: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.datasetcatalogbyname
            val groups = CredentialManager.readCredentialFromRequest(currentRequest).groups.toList
            val catalog = ServiceRegistry.catalogService.catalogByName(name, groups)

            /*
            val resutl  = catalog match {
                case MetaCatalog(None,None,None) => Datasetcatalogbyid401("Error no data with that logical_uri")
                case  _ =>  Datasetcatalogbyid200(catalog)
            }
            resutl
            */

            catalog match {
                case Some(c) => Datasetcatalogbyname200(c)
                case None => Datasetcatalogbyname401("Error")
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.datasetcatalogbyname
        }
        val autocompletedataset = autocompletedatasetAction { input: (MetadataCat, ResourceSize) =>
            val (q, limit) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.autocompletedataset
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            val datasetsFuture: Future[JsResult[Seq[AutocompRes]]] = CkanRegistry.ckanService.autocompleteDatasets(input, Option(credentials.username))
            val eitherDatasets: Future[Either[String, Seq[AutocompRes]]] = datasetsFuture.map(result => {
                result match {
                    case s: JsSuccess[Seq[AutocompRes]] => Right(s.get)
                    case e: JsError => Left( WebServiceUtil.getMessageFromJsError(e) )
                }
            })

            eitherDatasets.flatMap {
                case Right(autocomp) => Autocompletedataset200(autocomp)
                case Left(error) => Autocompletedataset401(Error(error,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.autocompletedataset
        }
        val isPresentOnCatalog = isPresentOnCatalogAction { (name: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.isPresentOnCatalog
            val result = ServiceRegistry.catalogRepository.isDatasetOnCatalog(name)
            result match {
                case Some(true) => IsPresentOnCatalog200(Success("is present", Some(name)))
                case _ => IsPresentOnCatalog401(s"$name non presente")

            }          //  NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.isPresentOnCatalog
        }
        val createdatasetcatalog = createdatasetcatalogAction { (catalog: MetaCatalog) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.createdatasetcatalog
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            if( CredentialManager.isDafEditor(currentRequest) || CredentialManager.isDafAdmin(currentRequest) ) {
                val created: Success = ServiceRegistry.catalogService.createCatalog(catalog, Option(credentials.username), ws)
                Createdatasetcatalog200(created)
            }else
                Createdatasetcatalog401("Admin or editor permissions required")
            //NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createdatasetcatalog
        }
        val test = testAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.test
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.test
        }
        val verifycredentials = verifycredentialsAction { (credentials: Credentials) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.verifycredentials
            CkanRegistry.ckanService.verifyCredentials(credentials) match {
                case true => Verifycredentials200(Success("Success", Some("User verified")))
                case _ =>  Verifycredentials401(Error("Wrong Username or Password",None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.verifycredentials
        }
        val voc_dcatthemegetall = voc_dcatthemegetallAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcatthemegetall
            val themeList: Seq[KeyValue] = VocServiceRegistry.vocRepository.listDcatThemeAll()
            Voc_dcatthemegetall200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcatthemegetall
        }
        val createckandataset = createckandatasetAction { (dataset: Dataset) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.createckandataset
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.DatasetWrites.writes(dataset)

            CkanRegistry.ckanService.createDataset(jsonv, Option(credentials.username))flatMap {
                case "true" => Createckandataset200(Success("Success", Some("dataset created")))
                case e =>  Createckandataset401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createckandataset
        }
        val getckandatasetListWithRes = getckandatasetListWithResAction { input: (ResourceSize, ResourceSize) =>
            val (limit, offset) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckandatasetListWithRes
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val datasetsFuture: Future[JsResult[Seq[Dataset]]] = CkanRegistry.ckanService.getDatasetsWithRes(input, Option(credentials.username))
            val eitherDatasets: Future[Either[String, Seq[Dataset]]] = datasetsFuture.map(result => {
                result match {
                    case s: JsSuccess[Seq[Dataset]] => Right(s.get)
                    case e: JsError => Left( WebServiceUtil.getMessageFromJsError(e))
                }
            })
            // Getckandatasetbyid200(dataset)
            eitherDatasets.flatMap {
                case Right(dataset) => GetckandatasetListWithRes200(dataset)
                case Left(error) => GetckandatasetListWithRes401(Error(error,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.getckandatasetListWithRes
        }
        val getckanuserorganizationList = getckanuserorganizationListAction { (username: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckanuserorganizationList
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val orgsFuture: Future[JsResult[Seq[Organization]]] = CkanRegistry.ckanService.getUserOrganizations(username, Option(credentials.username))
            val eitherOrgs: Future[Either[String, Seq[Organization]]] = orgsFuture.map(result => {
                result match {
                    case s: JsSuccess[Seq[Organization]] => Right(s.get)
                    case e: JsError => Left( WebServiceUtil.getMessageFromJsError(e) )
                }
            })
            // Getckandatasetbyid200(dataset)
            eitherOrgs.flatMap {
                case Right(orgs) => GetckanuserorganizationList200(orgs)
                case Left(error) => GetckanuserorganizationList401(Error(error,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.getckanuserorganizationList
        }
        val voc_themesgetall = voc_themesgetallAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_themesgetall
            val themeList: Seq[KeyValue] = VocServiceRegistry.vocRepository.listThemeAll()
            Voc_themesgetall200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_themesgetall
        }
        val voc_dcatsubthemesgetall = voc_dcatsubthemesgetallAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcatsubthemesgetall
            val themeList: Seq[VocKeyValueSubtheme] = VocServiceRegistry.vocRepository.listSubthemeAll()
            Voc_dcatsubthemesgetall200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcatsubthemesgetall
        }
        val voc_daf2dcatsubtheme = voc_daf2dcatsubthemeAction { input: (String, String) =>
            val (themeid, subthemeid) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcatsubtheme
            val subthemeList: Seq[VocKeyValueSubtheme] = VocServiceRegistry.vocRepository.daf2dcatSubtheme(input._1, input._2)
            Voc_daf2dcatsubtheme200(subthemeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcatsubtheme
        }
        val createckanorganization = createckanorganizationAction { (organization: Organization) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.createckanorganization
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.createOrganization(jsonv, Option(credentials.username))flatMap {
                case "true" => Createckanorganization200(Success("Success", Some("organization created")))
                case e =>  Createckanorganization401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createckanorganization
        }
        val updateckanorganization = updateckanorganizationAction { input: (String, Organization) =>
            val (org_id, organization) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.updateckanorganization
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.updateOrganization(org_id,jsonv, Option(credentials.username))flatMap {
                case "true" => Updateckanorganization200(Success("Success", Some("organization updated")))
                case e =>  Updateckanorganization401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.updateckanorganization
        }
        val getckanuser = getckanuserAction { (username: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckanuser
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val userResult: JsResult[User] = CkanRegistry.ckanService.getMongoUser(username, Option(credentials.username))
            val eitherUser: Either[String, User] = userResult match {
                case s: JsSuccess[User] => Right(s.get)
                case e: JsError => Left("error, no user with that name")
            }


            eitherUser match {
                case Right(user) => Getckanuser200(user)
                case Left(error) => Getckanuser401(Error(error,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.getckanuser
        }
        val createckanuser = createckanuserAction { (user: User) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.createckanuser
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.UserWrites.writes(user)
            CkanRegistry.ckanService.createUser(jsonv, Option(credentials.username))flatMap {
                case "true" => Createckanuser200(Success("Success", Some("user created")))
                case e =>  Createckanuser401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createckanuser
        }
        val getckandatasetbyid = getckandatasetbyidAction { (dataset_id: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckandatasetbyid
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val datasetFuture: Future[JsResult[Dataset]] = CkanRegistry.ckanService.testDataset(dataset_id, Option(credentials.username))
            val eitherDataset: Future[Either[String, Dataset]] = datasetFuture.map(result => {
                result match {
                    case s: JsSuccess[Dataset] => Right(s.get)
                    case e: JsError => Left( WebServiceUtil.getMessageFromJsError(e) )
                }
            })

            eitherDataset.flatMap {
                case Right(dataset) => Getckandatasetbyid200(dataset)//Getckandatasetbyid200(dataset)
                case Left(error) => Getckandatasetbyid401(Error(error,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.getckandatasetbyid
        }
        val voc_dcat2Daftheme = voc_dcat2DafthemeAction { (themeid: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcat2Daftheme
            val themeList: Seq[KeyValue] = VocServiceRegistry.vocRepository.dcat2DafTheme(themeid)
            Voc_dcat2Daftheme200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcat2Daftheme
        }
        val patchckanorganization = patchckanorganizationAction { input: (String, Organization) =>
            val (org_id, organization) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.patchckanorganization
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.patchOrganization(org_id,jsonv, Option(credentials.username))flatMap {
                case "true" => Patchckanorganization200(Success("Success", Some("organization patched")))
                case e =>  Patchckanorganization401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.patchckanorganization
        }
        val datasetcatalogbyid = datasetcatalogbyidAction { (catalog_id: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.datasetcatalogbyid
            val logical_uri = new java.net.URI(catalog_id)
            val catalog = ServiceRegistry.catalogService.catalog(logical_uri.toString)
            Logger.debug("*******")
            Logger.debug(logical_uri.toString)
            Logger.debug(catalog.toString)
            /*
            val resutl  = catalog match {
                case MetaCatalog(None,None,None) => Datasetcatalogbyid401("Error no data with that logical_uri")
                case  _ =>  Datasetcatalogbyid200(catalog)
            }
            resutl
            */

            catalog match {
                case Some(c) => Datasetcatalogbyid200(c)
                case None => Datasetcatalogbyid401("Error")
            }

            //Datasetcatalogbyid200(catalog.get)

             //NotImplementedYet
            //println("ale")
            //println(MetaCatalog(None,None,None))
            //Datasetcatalogbyid200(MetaCatalog(None,None,None))

            //Datasetcatalogbyid200(catalog.toString)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.datasetcatalogbyid
        }
        val voc_daf2dcattheme = voc_daf2dcatthemeAction { (themeid: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcattheme
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcattheme
        }
        val voc_dcatsubthemesgetbyid = voc_dcatsubthemesgetbyidAction { (themeid: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcatsubthemesgetbyid
            val themeList: Seq[KeyValue] = VocServiceRegistry.vocRepository.listDcatSubtheme(themeid)
            Voc_dcatsubthemesgetbyid200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcatsubthemesgetbyid
        }
        val publicdatasetcatalogbyname = publicdatasetcatalogbynameAction { (name: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.publicdatasetcatalogbyname
            val catalog = ServiceRegistry.catalogService.publicCatalogByName(name)


            catalog match {
                case Some(c) => Publicdatasetcatalogbyname200(c)
                case None => Publicdatasetcatalogbyname401("Error")
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.publicdatasetcatalogbyname
        }
        val getckanorganizationList = getckanorganizationListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckanorganizationList
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val eitherOut: Future[Either[Error, Seq[String]]] = CkanRegistry.ckanService.getOrganizations(Option(credentials.username)).map(result =>{
                result match {
                    case s: JsArray => Right(s.as[Seq[String]])
                    case _ => Left(GENERIC_ERROR)
                }
            })

            eitherOut.flatMap {
                case Right(list) => GetckanorganizationList200(list)
                case Left(error) => GetckanorganizationList401(error)
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.getckanorganizationList
        }
        val startKyloFedd = startKyloFeddAction { input: (String, MetaCatalog) =>
            val (file_type, feed) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.startKyloFedd
            val skipHeader = file_type match {
                case "csv" => true
                case "json" => false
            }


            // TODO use only one match case
            val ingest = feed.operational.input_src.sftp match {
                case None => "ws"
                case Some(_) => "sftp"
            }

            val templateProperties = ingest match {
                case "sftp" => kylo.datasetIngest(file_type, feed)
                case "ws" => kylo.wsIngest(file_type, feed)
            }

            val categoryFuture = kylo.categoryFuture(feed)

            val streamKyloTemplate = new FileInputStream(Environment.simple().getFile("/data/kylo/template.json"))

            val kyloTemplate  = try {
                    Json.parse(streamKyloTemplate)
            } finally {
                streamKyloTemplate.close()
            }

            val user = CredentialManager.readCredentialFromRequest(currentRequest).username

            val domain = feed.operational.theme
            val subDomain = feed.operational.subtheme
            val dsName = feed.dcatapit.name

            val sftPath =  URLEncoder.encode(s"/home/$user/$domain/$subDomain/$dsName", "UTF-8")

            logger.info(currentRequest.headers.get("authorization").get)

            val createDir = ws.url("http://security-manager.default.svc.cluster.local:9000/security-manager/v1/sftp/init/" + sftPath)
                  .withHeaders(("authorization", currentRequest.headers.get("authorization").get))

            //val trasformed = kyloTemplate.transform(KyloTrasformers.feedTrasform(feed))

            val kyloSchema = feed.dataschema.kyloSchema.get
            val inferJson = Json.parse(kyloSchema)

            val feedCreation  = ws.url(KYLOURL + "/api/v1/feedmgr/feeds")
              .withAuth(KYLOUSER, KYLOPWD, WSAuthScheme.BASIC)

            val feedData = for {
                (template, templates) <- templateProperties
                created <-  createDir.get()
                category <- categoryFuture
                trasformed <-  Future(kyloTemplate.transform(
                    KyloTrasformers.feedTrasform(feed,
                        template,
                        templates,
                        inferJson,
                        category,
                        file_type,
                        skipHeader)
                    )
                )
            } yield trasformed

            val createFeed: Future[WSResponse] = feedData.flatMap {
                case s: JsSuccess[JsValue] => logger.debug(Json.stringify(s.get));feedCreation.post(s.get)
                case e: JsError => throw new Exception(JsError.toJson(e).toString())
            }

            val result = createFeed.flatMap {
                // Assuming status 200 (OK) is a valid result for you.
                case resp : WSResponse if resp.status == 200 => logger.debug(Json.stringify(resp.json));StartKyloFedd200(yaml.Success("Feed started", Option(resp.body)))
                case _ => StartKyloFedd401(Error("Feed not created", Option(401), None))
            }

          result
           // NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.startKyloFedd
        }
    
     // Dead code for absent methodCatalog_managerYaml.deleteMetaCatalog
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.deleteMetaCatalog
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.deleteMetaCatalog
     */

    
     // Dead code for absent methodCatalog_managerYaml.deleteCatalog1
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.deleteCatalog1
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.deleteCatalog1
     */

    
    }
}
