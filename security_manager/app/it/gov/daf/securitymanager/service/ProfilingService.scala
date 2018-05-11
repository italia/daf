package it.gov.daf.securitymanager.service


import com.google.inject.{Inject, Singleton}
import security_manager.yaml.{AclPermission, Error, Success}

import scala.concurrent.Future
import cats.implicits._
import ProcessHandler._
import cats.data.EitherT
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.service.utilities.RequestContext._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, JsError, JsSuccess}
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


  private def listHDFSFolder(datasetPath:String):Future[Either[Error,List[(String,Boolean)]]] = {

    val queryString: Map[String, String] =Map("op"->"LISTSTATUS")

    webHDFSApiProxy.callHdfsService("GET",datasetPath,queryString) map{
      case Right(r) =>Right {
        (r.jsValue \ "FileStatuses" \ "FileStatus").as[JsArray].value.toList map { jsv =>
          val fileName = (jsv \ "pathSuffix").as[String]
          val fileType = (jsv \ "type").as[String] != "FILE"
          (fileName, fileType)
        }
      }
      case Left(l) => Left(Error(Option(0), Some(l.jsValue.toString()), None))
    }

  }

  private def setDatasetHDFSPermission(datasetPath:String, isDirectory:Boolean,setHdfsPerm:(String,Boolean)=>Future[Either[Error,Success]]) :Future[Either[Error,Success]] = {

    val res = for {

      a <- EitherT ( setHdfsPerm(datasetPath, isDirectory) )

      lista <- EitherT( listHDFSFolder(datasetPath) )

      v <- EitherT(
          lista.foldLeft[Future[Either[Error,Success]]](Future.successful(Right{Success(Some(""), Some(""))})) { (a, listElem) =>
            a flatMap {
              case Right(r) =>  if (listElem._2) setDatasetHDFSPermission(s"$datasetPath/${listElem._1}", listElem._2, setHdfsPerm)
                                else setHdfsPerm(s"$datasetPath/${listElem._1}",listElem._2)
              case Left(l) => Future.successful( Left(l) )
            }
          }
      )

    }yield v

    res.value
  }


  private def createHDFSPermission(datasetPath:String, isDirectory:Boolean, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    val subject = if(groupType != "user") "group" else "user"
    val aclString = if(isDirectory) s"$subject:$groupName:$permission,default:$subject:$groupName:$permission"
                    else s"$subject:$groupName:$permission"

    val queryString: Map[String, String] =Map("op"->"MODIFYACLENTRIES","aclspec"->aclString)

    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString) map{
      case Right(r) => Right( Success(Some("HDFS ACL updated"),Some("ok")) )
      case Left(l) => Left(Error(Option(0), Some(l.jsValue.toString()), None))
    }

  }

  def addPermissionToACL(datasetName:String, groupName:String, groupType:String, permission:String) :Future[Either[Error,Success]] = {

    val createPermission:(String,Boolean)=>Future[Either[Error,Success]] = createHDFSPermission(_, _, groupName, groupType, permission)

    val result = for {

      info <- stepOverF(Try {getDatasetInfo(datasetName)}); (datasetPath,owner)=info

      s <- stepOverF(testUser(owner))

      a <- step(Try {setDatasetHDFSPermission(datasetPath, true, createPermission)})
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

  private def revokeHDFSPermission(datasetPath:String, isDirectory:Boolean, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    val subject = if(groupType != "user") "group" else "user"
    val aclString = if(isDirectory) s"$subject:$groupName:,default:$subject:$groupName:"
                    else s"$subject:$groupName:"


    val queryString: Map[String, String] =Map("op"->"REMOVEACLENTRIES","aclspec"->aclString)

    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString) map{
      case Right(r) => Right( Success(Some("HDFS ACL deleted"),Some("ok")) )
      case Left(l) => Left(Error(Option(0), Some(l.jsValue.toString()), None))
    }

  }

  def deletePermissionToACL(datasetName:String, groupName:String, groupType:String, permission:String) :Future[Either[Error,Success]] = {

    val deletePermission:(String,Boolean)=>Future[Either[Error,Success]] = revokeHDFSPermission(_, _, groupName, groupType, permission)

    val result = for {

      info <- stepOverF(Try {getDatasetInfo(datasetName)}); (datasetPath,owner)=info

      s <- stepOverF(testUser(owner))

      a <- step(Try {setDatasetHDFSPermission(datasetPath, true, deletePermission)})
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

