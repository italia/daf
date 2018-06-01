package it.gov.daf.securitymanager.service


import com.google.inject.{Inject, Singleton}
import security_manager.yaml.{AclPermission, Error, Success}
import scala.concurrent.Future
import cats.implicits._
import ProcessHandler.{stepOverF, _}
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

    evalInFuture1{
      if(owner == getUsername()) Right("user is the owner")
      else Left("The user is not the owner of the dataset")
    }

  }

  private def getDatasetInfo(datasetPath:String ):Future[Either[Error,(String,String)]] = {

    evalInFuture0( MongoService.getDatasetInfo(datasetPath) )
  }

  private def addAclToCatalog(datasetName:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    evalInFuture0S( MongoService.addACL(datasetName, groupName, groupType, permission) )
  }


  private def createImpalaGrant(datasetPath:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    evalInFuture0S{
      val tableName = toTableName(datasetPath)
      impalaService.createGrant(tableName, groupName, permission)
    }

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

  private def setDatasetHDFSPermission(datasetPath:String, isDirectory:Boolean, setHdfsPerm:(String,Boolean)=>Future[Either[Error,Success]]) :Future[Either[Error,Success]] = {

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

  private def verifyPermissionString(permission:String):Future[Either[Error,Success]]={

    wrapInFuture1S{
      if (Permission.permissions.map(_.toString) contains (permission)) Right("ok")
      else Left(s"Permisson cab be: ${Permission.permissions}")
    }

  }

  def setACLPermission(datasetName:String, groupName:String, groupType:String, permission:String) :Future[Either[Error,Success]] = {

    val createPermission:(String,Boolean)=>Future[Either[Error,Success]] = createHDFSPermission(_, _, groupName, groupType, permission)

    val result = for {

      test <- stepOverF( verifyPermissionString(permission) )
      info <- stepOverF( getDatasetInfo(datasetName) ); (datasetPath,owner)=info

      s <- stepOverF( testUser(owner) )

      k <- stepOverF( deletePermissionIfPresent(datasetPath, groupName, groupType) )

      a <- step( setDatasetHDFSPermission(datasetPath, true, createPermission) )
      b <- step( a, createImpalaGrant(datasetPath, groupName, groupType, permission) )
      c <- step( b, addAclToCatalog(datasetName, groupName, groupType, permission) )

    } yield c

    result.value.map {
      case Right(r) => Right(Success(Some("ACL added"), Some("ok")))
      case Left(l) => if( l.steps !=0 ) {
        hardDeletePermissionFromACL(datasetName, groupName, groupType).onSuccess { case e =>

          val steps = e.fold(ll=>ll.steps,rr=>rr.steps)
          if( l.steps != steps)
            throw new Exception( s"setACLPermission rollback issue: process steps=${l.steps} rollback steps=$steps" )

        }

      }
        Left(l.error)
    }

  }


  private def deleteAclFromCatalog(datasetName:String, groupName:String, groupType:String ):Future[Either[Error,Success]] = {

    evalInFuture0S( MongoService.removeAllACL(datasetName, groupName, groupType) )
  }


  private def revokeImpalaGrant(datasetPath:String, groupName:String, groupType:String):Future[Either[Error,Success]] = {

    evalInFuture0S{
      val tableName = toTableName(datasetPath)
      impalaService.revokeGrant(tableName, groupName)
    }
  }

  private def revokeHDFSPermission(datasetPath:String, isDirectory:Boolean, groupName:String, groupType:String):Future[Either[Error,Success]] = {

    val subject = if(groupType != "user") "group" else "user"
    val aclString = if(isDirectory) s"$subject:$groupName:,default:$subject:$groupName:"
                    else s"$subject:$groupName:"


    val queryString: Map[String, String] =Map("op"->"REMOVEACLENTRIES","aclspec"->aclString)

    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString) map{
      case Right(r) => Right( Success(Some("HDFS ACL deleted"),Some("ok")) )
      case Left(l) => Left(Error(Option(0), Some(l.jsValue.toString()), None))
    }

  }

  private def hardDeletePermissionFromACL(datasetPath:String, groupName:String, groupType:String) :Future[Either[ErrorWrapper,SuccessWrapper]] = {

    val deletePermission:(String,Boolean)=>Future[Either[Error,Success]] = revokeHDFSPermission(_, _, groupName, groupType)
    val datasetName = toDatasetName(datasetPath)

    val result = for {

      a <- step( setDatasetHDFSPermission(datasetPath, true, deletePermission) )
      b <- step(a, revokeImpalaGrant(datasetPath, groupName, groupType) )
      c <- step(b, deleteAclFromCatalog(datasetName, groupName, groupType) )

    } yield c


    result.value

  }

  def deletePermissionFromACL(datasetName:String, groupName:String, groupType:String) :Future[Either[Error,Success]] = {


    val result = for {

      info <- stepOverF( getDatasetInfo(datasetName) ) ; (datasetPath,owner)=info

      s <- stepOverF( testUser(owner) )

      a <- EitherT( hardDeletePermissionFromACL(datasetPath, groupName, groupType) )

    } yield a

    result.value map{
      case Right(r) =>Right(Success(Some("ACL deleted"),Some("ok")))
      case Left(l) => if( l.steps == 0 ) Left(l.error)
                      else throw new Exception( s"deletePermissionFromACL process issue: process steps=${l.steps}" )
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

  private def checkPermissions(datasetName:String) :Future[Either[Error,Success]] = {

    getPermissions(datasetName).map{
                                      case Right(r) =>  if(r.isEmpty || r.length==0) Left(Error(Option(1), None, None))
                                                        else Right{Success(Some(""), Some(""))}
                                      case Left(l) => Left(l)
                                    }
  }

  def deletePermissionIfPresent(datasetPath:String, groupName:String, groupType:String) :Future[Either[Error,Success]] = {

    val out = checkPermissions(toDatasetName(datasetPath))

    out flatMap { case Right(r) => hardDeletePermissionFromACL(datasetPath, groupName, groupType) map{
                                                                                                      case Right(r) =>Right(r.success)
                                                                                                      case Left(l) => if( l.steps == 0 ) Left(l.error)
                                                                                                      else throw new Exception( s"deletePermissionIfPresent process issue: process steps=${l.steps}" )
                                                                                                      }
                  case Left(Error(Some(1), None, None))=> Future.successful( Right{Success(Some("Nothing todo"), Some("ok"))} )
                  case _ => out
                }

  }

  def toTableName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').drop(4).mkString(".")
  def toDatasetName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').last.split("_o_").last

}

