package it.gov.daf.catalogmanager.service

/**
  * Created by ale on 18/07/17.
  */
import catalog_manager.yaml.{Credentials, Dataset, MetadataCat, Organization, ResourceSize, User}
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

    def getMongoUser(name:String): JsResult[User]  = {
      ckanRepository.getMongoUser(name)
    }

    def verifyCredentials(credentials: Credentials):Boolean = {
      ckanRepository.verifyCredentials(credentials: Credentials)
    }
    def updateOrganization(orgId: String, jsonOrg: JsValue): Future[String] = {
      ckanRepository.updateOrganization(orgId,jsonOrg)
    }
    def createUser(jsonUser: JsValue): Future[String] = {
      ckanRepository.createUser(jsonUser)
    }
    def getUserOrganizations(userName :String) : Future[JsResult[Seq[Organization]]] = {
      ckanRepository.getUserOrganizations(userName)
    }

    def createDataset(jsonDataset: JsValue): Future[String] = {
      ckanRepository.createDataset(jsonDataset)
    }
    def createOrganization(jsonDataset: JsValue): Future[String] = {
      ckanRepository.createOrganization(jsonDataset)
    }
    def dataset(datasetId: String): JsValue = {
      ckanRepository.dataset(datasetId)
    }

    def getOrganization(orgId :String) : Future[JsResult[Organization]] = {
      ckanRepository.getOrganization(orgId)
    }

    def getOrganizations() : Future[JsValue] = {
      ckanRepository.getOrganizations
    }

    def getDatasets() : Future[JsValue] = {
      ckanRepository.getDatasets
    }

    def searchDatasets( input: (MetadataCat, MetadataCat, ResourceSize) ) : Future[JsResult[Seq[Dataset]]] = {
      ckanRepository.searchDatasets(input)
    }

    def getDatasetsWithRes( input: (ResourceSize, ResourceSize) ) : Future[JsResult[Seq[Dataset]]] = {
      ckanRepository.getDatasetsWithRes(input)
    }

    def testDataset(datasetId :String) : Future[JsResult[Dataset]] = {
      ckanRepository.testDataset(datasetId)
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
