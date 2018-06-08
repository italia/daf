package it.gov.daf.securitymanager.service

import com.google.inject.Inject
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.Logger
import play.api.libs.json.{JsUndefined, JsValue, Json}
import play.api.libs.ws.{WSAuthScheme, WSClient}
import security_manager.yaml.{Error, Success}

import scala.concurrent.Future

class KyloApiClient @Inject()(wSClient: WSClient){

  import play.api.libs.concurrent.Execution.Implicits._

  def createCategory(name: String):Future[Either[Error,Success]]= {

    val jsonRequest: JsValue = Json.parse(
                                        s"""{
                                           "id": null,
                                           "name": "$name",
                                           "description": null,
                                           "icon": null,
                                           "iconColor": null,
                                           "userFields": [],
                                           "userProperties": [],
                                           "relatedFeedSummaries": [],
                                           "securityGroups": [],
                                           "roleMemberships": [],
                                           "feedRoleMemberships": [],
                                           "owner": null,
                                           "systemName": "$name"
                                    }""")

    Logger.logger.debug("createCategory: "+ jsonRequest.toString())

    //throw new java.net.UnknownHostException("weee")

    val response = wSClient.url(ConfigReader.kyloUrl + "/proxy/v1/feedmgr/categories")
                    .withHeaders("Accept" -> "application/json")
                    .withAuth(ConfigReader.kyloUser,ConfigReader.kyloUserPwd,WSAuthScheme.BASIC)
                    .post(jsonRequest)

    response.map{response =>

      if( response.status != 200 )
        Left( Error(Option(0),Some("Error in during kylo category creation: bad http code"+response.status),None) )
      else{
        Logger.logger.debug("RESPONSE:"+response.json)
        val result = response.json \ "id"
        if( result.isInstanceOf[JsUndefined] )
          Left( Error(Option(0),Some("Error in during kylo category creation"),None) )
        else
          Right( Success(Some("Category created"), Some("ok")) )
      }

    }

  }


}
