
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

        // ----- End of unmanaged code area for constructor Catalog_managerYaml
        val autocompletedummy = autocompletedummyAction { (autocompRes: AutocompRes) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.autocompletedummy
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.autocompletedummy
        }
        val searchdataset = searchdatasetAction { input: (MetadataCat, MetadataCat, ResourceSize, ResourceSize) =>
            val (q, sort, rows, start) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.searchdataset
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

            //if( ! CkanRegistry.ckanService.verifyCredentials(credentials) )
            //Searchdataset401(Error(None,Option("Invalid credentials!"),None))

            val datasetsFuture: Future[JsResult[Seq[Dataset]]] = CkanRegistry.ckanService.searchDatasets(input, credentials._1)
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
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val orgFuture: Future[JsResult[Organization]] = CkanRegistry.ckanService.getOrganization(org_id, credentials._1)
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
        val getckandatasetList = getckandatasetListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckandatasetList
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val eitherOut: Future[Either[Error, Seq[String]]] = CkanRegistry.ckanService.getDatasets(credentials._1).map(result =>{
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
        val autocompletedataset = autocompletedatasetAction { input: (MetadataCat, ResourceSize) =>
            val (q, limit) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.autocompletedataset
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)

            val datasetsFuture: Future[JsResult[Seq[AutocompRes]]] = CkanRegistry.ckanService.autocompleteDatasets(input, credentials._1)
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
        val createdatasetcatalog = createdatasetcatalogAction { (catalog: MetaCatalog) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.createdatasetcatalog
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val created: Success = ServiceRegistry.catalogService.createCatalog(catalog, credentials._1, ws )
            if (!created.message.toLowerCase.equals("error")) {
                val logicalUri = created.message
                val logicalUriEncoded = URLEncoder.encode(logicalUri, "UTF-8")
                val ingestionUrl = ConfigReader.ingestionUrl
                val wsResponse = ws.url(ingestionUrl + "/ingestion-manager/v1/add-new-dataset/" + logicalUriEncoded)
                    .withHeaders(("authorization",currentRequest.headers.get("authorization").get))
                    .get()
                wsResponse.map(x => println(x.body))
             //   ingestionListener.addDirListener(catalog, logicalUri)
            }
           // ws.url("http://www.google.com").get().map( x => println(x.body))

            Createdatasetcatalog200(created)
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
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.DatasetWrites.writes(dataset)

            CkanRegistry.ckanService.createDataset(jsonv, credentials._1)flatMap {
                case "true" => Createckandataset200(Success("Success", Some("dataset created")))
                case e =>  Createckandataset401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createckandataset
        }
        val getckandatasetListWithRes = getckandatasetListWithResAction { input: (ResourceSize, ResourceSize) =>
            val (limit, offset) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckandatasetListWithRes
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val datasetsFuture: Future[JsResult[Seq[Dataset]]] = CkanRegistry.ckanService.getDatasetsWithRes(input, credentials._1)
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
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val orgsFuture: Future[JsResult[Seq[Organization]]] = CkanRegistry.ckanService.getUserOrganizations(username, credentials._1)
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
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.createOrganization(jsonv, credentials._1)flatMap {
                case "true" => Createckanorganization200(Success("Success", Some("organization created")))
                case e =>  Createckanorganization401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createckanorganization
        }
        val updateckanorganization = updateckanorganizationAction { input: (String, Organization) =>
            val (org_id, organization) = input
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.updateckanorganization
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.updateOrganization(org_id,jsonv, credentials._1)flatMap {
                case "true" => Updateckanorganization200(Success("Success", Some("organization updated")))
                case e =>  Updateckanorganization401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.updateckanorganization
        }
        val getckanuser = getckanuserAction { (username: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckanuser
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val userResult: JsResult[User] = CkanRegistry.ckanService.getMongoUser(username, credentials._1)
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
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.UserWrites.writes(user)
            CkanRegistry.ckanService.createUser(jsonv, credentials._1)flatMap {
                case "true" => Createckanuser200(Success("Success", Some("user created")))
                case e =>  Createckanuser401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createckanuser
        }
        val getckandatasetbyid = getckandatasetbyidAction { (dataset_id: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckandatasetbyid
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val datasetFuture: Future[JsResult[Dataset]] = CkanRegistry.ckanService.testDataset(dataset_id, credentials._1)
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
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.patchOrganization(org_id,jsonv, credentials._1)flatMap {
                case "true" => Patchckanorganization200(Success("Success", Some("organization patched")))
                case e =>  Patchckanorganization401(Error(e,None,None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.patchckanorganization
        }
        val datasetcatalogbyid = datasetcatalogbyidAction { (catalog_id: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.datasetcatalogbyid
            val logical_uri = new java.net.URI(catalog_id)
            val catalog = ServiceRegistry.catalogService.catalog(logical_uri.toString)
            println("*******")
            println(logical_uri.toString)
            println(catalog.toString)
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
        val getckanorganizationList = getckanorganizationListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.getckanorganizationList
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val eitherOut: Future[Either[Error, Seq[String]]] = CkanRegistry.ckanService.getOrganizations(credentials._1).map(result =>{
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
        val datasetcatalogbytitle = datasetcatalogbytitleAction { (title: String) =>  
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.datasetcatalogbytitle
            val catalog = ServiceRegistry.catalogService.catalogBytitle(title)

            /*
            val resutl  = catalog match {
                case MetaCatalog(None,None,None) => Datasetcatalogbyid401("Error no data with that logical_uri")
                case  _ =>  Datasetcatalogbyid200(catalog)
            }
            resutl
            */

            catalog match {
                case Some(c) => Datasetcatalogbytitle200(c)
                case None => Datasetcatalogbytitle401("Error")
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.datasetcatalogbytitle
        }
    
     // Dead code for absent methodCatalog_managerYaml.createIPAuser
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.createIPAuser
            ApiClientIPA.createUser(user) flatMap {
                case Right(success) => CreateIPAuser200(success)
                case Left(err) => CreateIPAuser500(err)
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.createIPAuser
     */

    
     // Dead code for absent methodCatalog_managerYaml.voc_dcatap2dafsubtheme
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcatap2dafsubtheme
            val themeList: Seq[VocKeyValueSubtheme] = VocServiceRegistry.vocRepository.dcatapit2DafSubtheme(input._1, input._2)
            Voc_dcatap2dafsubtheme200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcatap2dafsubtheme
     */

    
     // Dead code for absent methodCatalog_managerYaml.inferschema
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.inferschema
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.inferschema
     */

    
     // Dead code for absent methodCatalog_managerYaml.voc_dcatapitthemegetall
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcatapitthemegetall
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcatapitthemegetall
     */

    
     // Dead code for absent methodCatalog_managerYaml.voc_daf2dcataptheme
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcataptheme
            val subthemeList: Seq[KeyValue] = VocServiceRegistry.vocRepository.daf2dcatapitTheme(themeid)
            Voc_daf2dcataptheme200(subthemeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcataptheme
     */

    
     // Dead code for absent methodCatalog_managerYaml.voc_dcatap2Daftheme
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_dcatap2Daftheme
            val themeList: Seq[KeyValue] = VocServiceRegistry.vocRepository.dcatapit2DafTheme(themeid)
            Voc_dcatap2Daftheme200(themeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_dcatap2Daftheme
     */

    
     // Dead code for absent methodCatalog_managerYaml.registrationconfirm
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.registrationconfirm
            RegistrationService.createUser(token) flatMap {
                case Right(success) => Registrationconfirm200(success)
                case Left(err) => Registrationconfirm500(err)
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.registrationconfirm
     */

    
     // Dead code for absent methodCatalog_managerYaml.tempo
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.tempo
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.tempo
     */

    
     // Dead code for absent methodCatalog_managerYaml.addDataset
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.addDataset
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.addDataset
     */

    
     // Dead code for absent methodCatalog_managerYaml.registrationrequest
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.registrationrequest
            val reg = RegistrationService.requestRegistration(user) flatMap {
                case Right(mailService) => mailService.sendMail()
                case Left(msg) => Future {Left(msg)}
            }

            reg flatMap {
                case Right(msg) => Registrationrequest200(Success(Some("Success"), Some(msg)))
                case Left(msg) => Registrationrequest500(Error(None, Option(msg), None))
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.registrationrequest
     */

    
     // Dead code for absent methodCatalog_managerYaml.voc_daf2dcatapsubtheme
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcatapsubtheme
            val subthemeList: Seq[VocKeyValueSubtheme] = VocServiceRegistry.vocRepository.daf2dcatapitSubtheme(input._1, input._2)
            Voc_daf2dcatapsubtheme200(subthemeList)
            // ----- End of unmanaged code area for action  Catalog_managerYaml.voc_daf2dcatapsubtheme
     */

    
     // Dead code for absent methodCatalog_managerYaml.showipauser
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.showipauser
            ApiClientIPA.showUser(uid) flatMap {
                case Right(success) => Showipauser200(success)
                case Left(err) => Showipauser500(err)
            }
            // ----- End of unmanaged code area for action  Catalog_managerYaml.showipauser
     */

    
     // Dead code for absent methodCatalog_managerYaml.ckandatasetbyid
     /*
                  // ----- Start of unmanaged code area for action  Catalog_managerYaml.ckandatasetbyid
                  val dataset: Future[Dataset] = ServiceRegistry.catalogService.getDataset(dataset_id)
                  Ckandatasetbyid200(dataset)
                  //NotImplementedYet
                  // ----- End of unmanaged code area for action  Catalog_managerYaml.ckandatasetbyid
     */

    
     // Dead code for absent methodCatalog_managerYaml.showIPAuser
     /*
            // ----- Start of unmanaged code area for action  Catalog_managerYaml.showIPAuser
            NotImplementedYet
            // ----- End of unmanaged code area for action  Catalog_managerYaml.showIPAuser
     */

    
    }
}
