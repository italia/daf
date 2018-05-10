package it.gov.daf.securitymanager.service


import com.google.inject.{Inject, Singleton}
import security_manager.yaml.{AclPermission, Error, Success}

import scala.concurrent.Future
import cats.implicits._
import ProcessHandler._
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.service.utilities.RequestContext._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess}
import security_manager.yaml.BodyReads._

import scala.util.{Left, Try}

@Singleton
class ProfilingService @Inject()(webHDFSApiProxy:WebHDFSApiProxy,impalaService:ImpalaService){


  private def testUser(owner:String ):Future[Either[Error,String]] = {

    def methodCall = if(owner == getUsername()) Right("user is the owner") else Left("The user is not the owner of the dataset")
    evalInFuture1(methodCall)
  }

  private def getDatasetInfo(datasetPath:String ):Future[Either[Error,(String,String)]] = {

    def methodCall = MongoService.getDatasetInfo(datasetPath)
    evalInFuture0(methodCall)
  }

  private def addAclToCatalog(datasetName:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    def methodCall = MongoService.addACL(datasetName, groupName, groupType, permission)
    evalInFuture0S(methodCall)
  }


  private def createImpalaGrant(datasetPath:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    val tableName = toTableName(datasetPath)

    def methodCall = impalaService.createGrant(tableName, groupName, permission)
    evalInFuture0S(methodCall)
  }

  private def createHDFSPermission(datasetPath:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    //curl -i -X PUT "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=MODIFYACLENTRIES
    //&aclspec=<ACLSPEC>"
    //ordinary/test_ingestion/TRAN__terrestre/test_ingestion_o_sample_incidenti_2?op=SETACL&aclspec=user:impala:rwx,user:hive:rwx,user::rw-,group:test_ingestion:rwx,group::---,other::---

    //group:sales:rwx
    //user:pippo:r--


    val subject = if(groupType != "user") "group" else "user"
    val aclString = s"$subject:$groupName:$permission,default:$subject:$groupName:$permission"
    val queryString: Map[String, String] =Map("op"->"MODIFYACLENTRIES","aclspec"->aclString)

    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString) map{
      case Right(r) => Right( Success(Some("HDFS ACL updated"),Some("ok")) )
      case Left(l) => Left(Error(Option(0), Some(l.jsValue.toString()), None))
    }

  }

  def addPermissionToACL(datasetName:String, groupName:String, groupType:String, permission:String) :Future[Either[Error,Success]] = {


    val result = for {

      info <- stepOverF(Try {getDatasetInfo(datasetName)}); (datasetPath,owner)=info

      s <- stepOverF(testUser(owner))

      a <- step(Try {createHDFSPermission(datasetPath, groupName, groupType, permission)})
      b <- step(a, Try {createImpalaGrant(datasetPath, groupName, groupType, permission)})
      c <- step(b, Try {addAclToCatalog(datasetName, groupName, groupType, permission)})

    } yield c

    result.value.map {
      case Right(r) => Right(Success(Some("ACL added"), Some("ok")))
      case Left(l) => Left(l.error)
    }

  }


  private def deleteAclFromCatalog(datasetName:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    def methodCall = MongoService.removeACL(datasetName, groupName, groupType, permission)
    evalInFuture0S(methodCall)
  }


  private def revokeImpalaGrant(datasetPath:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    val tableName = toTableName(datasetPath)

    def methodCall = impalaService.revokeGrant(tableName, groupName, permission)
    evalInFuture0S(methodCall)
  }

  private def revokeHDFSPermission(datasetPath:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    val subject = if(groupType != "user") "group" else "user"
    val aclString = s"$subject:$groupName:,default:$subject:$groupName:"

    //val aclString = s"${if(groupType != "user") "group" else "user"}:$groupName:"//$permission
    val queryString: Map[String, String] =Map("op"->"REMOVEACLENTRIES","aclspec"->aclString)

    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString) map{
      case Right(r) => Right( Success(Some("HDFS ACL deleted"),Some("ok")) )
      case Left(l) => Left(Error(Option(0), Some(l.jsValue.toString()), None))
    }

  }

  def deletePermissionToACL(datasetName:String, groupName:String, groupType:String, permission:String) :Future[Either[Error,Success]] = {


    val result = for {

      info <- stepOverF(Try {getDatasetInfo(datasetName)}); (datasetPath,owner)=info

      s <- stepOverF(testUser(owner))

      a <- step(Try {revokeHDFSPermission(datasetPath, groupName, groupType, permission)})
      b <- step(a, Try {revokeImpalaGrant(datasetPath, groupName, groupType, permission) })
      c <- step(b, Try {deleteAclFromCatalog(datasetName, groupName, groupType, permission)})

    } yield c


    result.value.map {
      case Right(r) => Right(Success(Some("ACL deleted"), Some("ok")))
      case Left(l) => Left(l.error)
    }

  }

  def getPermissions(datasetName:String) :Future[Either[Error,Seq[AclPermission]]] = {

    def methodCall = MongoService.getACL(datasetName)
    evalInFuture0(methodCall).map{
      case Right(json) => json.validate[Seq[AclPermission]] match {
        case s: JsSuccess[Seq[AclPermission]] => Right(s.get)
        case e: JsError => Left( Error(Option(1), Some(WebServiceUtil.getMessageFromJsError(e)), None) )
      }
      case Left(l) => Left(l)
    }
  }

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

        }*/


  def toTableName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').drop(4).mkString(".")
  //def toDatasetName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').last.split("_o_").last

}

