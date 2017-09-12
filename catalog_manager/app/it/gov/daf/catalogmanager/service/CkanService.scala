package it.gov.daf.catalogmanager.service

/**
  * Created by ale on 18/07/17.
  */
import catalog_manager.yaml.{AutocompRes, Credentials, Dataset, MetadataCat, Organization, ResourceSize, User}
import play.api.{Configuration, Environment}
import play.api.libs.json.{JsResult, JsValue}
import it.gov.daf.catalogmanager.repository.ckan.{CkanRepository, CkanRepositoryComponent}

import scala.concurrent.Future

/**
  * Created by ale on 01/07/17.
  */

trait CkanServiceComponent {
  this: CkanRepositoryComponent =>
  val ckanService: CkanService

  class CkanService {

    def getMongoUser(name:String, callingUserid :MetadataCat ): JsResult[User]  = {
      ckanRepository.getMongoUser(name, callingUserid)
    }

    def verifyCredentials(credentials: Credentials):Boolean = {
      ckanRepository.verifyCredentials(credentials: Credentials)
    }
    def updateOrganization(orgId: String, jsonOrg: JsValue, callingUserid :MetadataCat ): Future[String] = {
      ckanRepository.updateOrganization(orgId,jsonOrg, callingUserid)
    }
    def patchOrganization(orgId: String, jsonOrg: JsValue, callingUserid :MetadataCat ): Future[String] = {
      ckanRepository.patchOrganization(orgId,jsonOrg, callingUserid)
    }

    def createUser(jsonUser: JsValue, callingUserid :MetadataCat): Future[String] = {
      ckanRepository.createUser(jsonUser, callingUserid)
    }
    def getUserOrganizations(userName :String, callingUserid :MetadataCat) : Future[JsResult[Seq[Organization]]] = {
      ckanRepository.getUserOrganizations(userName, callingUserid)
    }

    def createDataset(jsonDataset: JsValue, callingUserid :MetadataCat): Future[String] = {
      ckanRepository.createDataset(jsonDataset,callingUserid)
    }
    def createOrganization(jsonDataset: JsValue, callingUserid :MetadataCat): Future[String] = {
      ckanRepository.createOrganization(jsonDataset,callingUserid)
    }
    def dataset(datasetId: String, callingUserid :MetadataCat): JsValue = {
      ckanRepository.dataset(datasetId,callingUserid)
    }

    def getOrganization(orgId :String, callingUserid :MetadataCat) : Future[JsResult[Organization]] = {
      ckanRepository.getOrganization(orgId,callingUserid)
    }

    def getOrganizations(callingUserid :MetadataCat) : Future[JsValue] = {
      ckanRepository.getOrganizations(callingUserid)
    }

    def getDatasets(callingUserid :MetadataCat) : Future[JsValue] = {
      ckanRepository.getDatasets(callingUserid)
    }

    def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize), callingUserid :MetadataCat) : Future[JsResult[Seq[Dataset]]] = {
      ckanRepository.searchDatasets(input, callingUserid)
    }
    def autocompleteDatasets( input: (MetadataCat, ResourceSize), callingUserid :MetadataCat) : Future[JsResult[Seq[AutocompRes]]] = {
      ckanRepository.autocompleteDatasets(input, callingUserid)
    }

    def getDatasetsWithRes( input: (ResourceSize, ResourceSize),callingUserid :MetadataCat ) : Future[JsResult[Seq[Dataset]]] = {
      ckanRepository.getDatasetsWithRes(input, callingUserid)
    }

    def testDataset(datasetId :String, callingUserid :MetadataCat) : Future[JsResult[Dataset]] = {
      ckanRepository.testDataset(datasetId, callingUserid)
    }

  }
}


object CkanRegistry extends
  CkanServiceComponent with
  CkanRepositoryComponent {
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val ckanRepository =  CkanRepository(app)
  val ckanService = new CkanService
}
