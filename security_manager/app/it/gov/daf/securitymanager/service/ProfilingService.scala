package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.authentication.Role
import it.gov.daf.sso.ApiClientIPA
import security_manager.yaml.{DafOrg, Error, IpaUser, Success}

import scala.concurrent.Future
import cats.implicits._
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import ProcessHandler._
import IntegrationService._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.Try

@Singleton
class ProfilingService @Inject()(webHDFSApiProxy:WebHDFSApiProxy,impalaService:ImpalaService){

  def addPermissionToACL(datasetPath:String, groupName:String, groupType:String, permission:String) :Future[Either[Error,Success]] = {

    //curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=MODIFYACLENTRIES
    //&aclspec=<ACLSPEC>"
    //ordinary/test_ingestion/TRAN__terrestre/test_ingestion_o_sample_incidenti_2?op=SETACL&aclspec=user:impala:rwx,user:hive:rwx,user::rw-,group:test_ingestion:rwx,group::---,other::---

    //group:sales:rwx
    //user:pippo:r--

    val datasetName = ProfilingService.toDatasetName(datasetPath)

    val aclString = s"${if(groupType != "user") "group" else "user"}:$groupName:$permission"
    val queryString: Map[String, String] =Map("op"->"MODIFYACLENTRIES","aclspec"->aclString)
    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString)

    val tableName = ProfilingService.toDatasetName(datasetPath)

    impalaService.createInsertGrant(tableName,groupName)

    MongoService.addACL(datasetName, groupName, groupType, permission)

    /*
    val groupCn = dafOrg.groupCn
    val predefinedOrgIpaUser =     IpaUser(groupCn,
      "predefined organization user",
      dafOrg.predefinedUserMail.getOrElse( toMail(groupCn) ),
      toUserName(groupCn),
      Option(Role.Editor.toString),
      Option(dafOrg.predefinedUserPwd),
      None,
      Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- step( Try{apiClientIPA.createGroup(dafOrg.groupCn)} )
      b <- step( a, Try{registrationService.checkMailNcreateUser(predefinedOrgIpaUser,true)} )
      c <- step( b, Try{supersetApiClient.createDatabase(toSupersetDS(groupCn),predefinedOrgIpaUser.uid,dafOrg.predefinedUserPwd,dafOrg.supSetConnectedDbName)} )
      d <- stepOver( c, Try{kyloApiClient.createCategory(dafOrg.groupCn)} )

      e <- step( c, Try{ckanApiClient.createOrganizationAsAdmin(groupCn)} )
      e1 <- step( c, Try{ckanApiClient.createOrganizationInGeoCkanAsAdmin(groupCn)} )

      g <- stepOver( e, Try{addUserToOrganization(groupCn,predefinedOrgIpaUser.uid)} )

    } yield g

    // 5 step

    result.value.map{
      case Right(r) => Right( Success(Some("Organization created"), Some("ok")) )
      case Left(l) => if( l.steps !=0 ) {
        hardDeleteDafOrganization(groupCn).onSuccess { case e =>

          val steps = e.fold(ll=>ll.steps,rr=>rr.steps)
          if( l.steps != steps)
            throw new Exception( s"CreateDafOrganization rollback issue: process steps=${l.steps} rollback steps=$steps" )

        }

      }
        Left(l.error)
    }
*/

  }


}


object ProfilingService {
  def toTableName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').dropRight(3).mkString(".")
  def toDatasetName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').last.split("_o_").last

}
