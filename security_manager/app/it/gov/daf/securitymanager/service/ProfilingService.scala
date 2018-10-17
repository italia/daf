package it.gov.daf.securitymanager.service


import com.google.inject.{Inject, Singleton}
import security_manager.yaml.{AclPermission, DafGroupInfo, Error, Success}

import scala.concurrent.Future
import cats.implicits._
import ProcessHandler.{stepOverF, _}
import cats.data.EitherT
import it.gov.daf.common.sso.common.{Admin, Role}
import it.gov.daf.common.utils.WebServiceUtil
import it.gov.daf.securitymanager.utilities.ConfigReader
import it.gov.daf.common.utils.RequestContext._
import it.gov.daf.sso.{ApiClientIPA, OPEN_DATA_GROUP}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, JsError, JsSuccess}
import security_manager.yaml.BodyReads._

import scala.util.{Left, Try}

@Singleton
class ProfilingService @Inject()(webHDFSApiProxy:WebHDFSApiProxy,impalaService:ImpalaService,integrationService: IntegrationService, apiClientIPA: ApiClientIPA,supersetApiClient: SupersetApiClient){


  private val logger = Logger(this.getClass.getName)

  private def testUser(owner:String):Future[Either[Error,String]] = {

    Logger.debug(s"testUser owner: $owner")

    evalInFuture1{
      if( owner == getUsername || (ConfigReader.hdfsAdminUser.size>0 && ConfigReader.hdfsAdminUser==getUsername) )
        Right("user is the owner")
      else
        Left("The user is not the owner of the dataset")
    }

  }


  private def testIfUserCanGivePermission(ownerOrg:String,groupName:String):Future[Either[Error,String]] = {

    def testIfGroupBelongsToOwnerOrg(parentOrg:Seq[DafGroupInfo]):Future[Either[Error,String]]  = {

      logger.debug(s"testIfGroupBelongsToOwnerOrg DafGroupInfo:  $parentOrg")

      def testIfUserIsOwnerOrgAdmin:Future[Either[Error,String]] = {

        val ownerOrgAdminRole:String=Admin+ownerOrg
        logger.debug(s"testIfUserIsOwnerOrgAdmin ownerOrgAdminRole: $ownerOrgAdminRole")
        apiClientIPA.findUser(Left(getUsername())).map {
          case Right(r) =>  if (r.roles.getOrElse(Seq.empty).contains(ownerOrgAdminRole)) Right("ok")
                            else{ logger.debug(s"user roles: ${r.roles.getOrElse(Seq.empty)}");Left(Error(Option(1), Some("The user not authorized to give permission outside his organization"), None))}
          case Left(l) => Left(l)
        }
      }


      parentOrg match{
        case Seq(DafGroupInfo(_,_,Some(`ownerOrg`),_)) => Future.successful(Right("ok"))
        case Seq(DafGroupInfo(_,_,Some(_),_)) | Seq(DafGroupInfo(_,_,None,_)) => testIfUserIsOwnerOrgAdmin
        case Seq() | Seq(_) => Future.successful(Left(Error(Option(0), Some("Error while collecting group info"), None)))
      }
    }


    // if the permission are given to others than dataset owner org..
    if(ownerOrg!=groupName){

      val out = for{
        groupInfo <- EitherT(apiClientIPA.groupsInfo(Seq(groupName)))
        b <- EitherT(testIfGroupBelongsToOwnerOrg(groupInfo))

      }yield b

      out.value

    }else Future.successful(Right("ok"))

  }

  private def getDatasetInfo(datasetPath:String ):Future[Either[Error,(String,String,String)]] = {

    Logger.debug(s"getDatasetInfo datasetPath: $datasetPath")

    evalInFuture0( MongoService.getDatasetInfo(datasetPath) )
  }

  private def addAclToCatalog(datasetName:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    Logger.debug(s"addAclToCatalog datasetName: $datasetName")

    evalInFuture0S( MongoService.addACL(datasetName, groupName, groupType, permission) )
  }


  private def createImpalaGrant(datasetPath:String, groupName:String, groupType:String, permission:String ):Future[Either[Error,Success]] = {

    Logger.debug(s"createImpalaGrant datasetPath: $datasetPath")

    evalInFuture0S{
      val tableName = toTableName(datasetPath)
      impalaService.createGrant(tableName, groupName, permission, false, false)
    }

  }


  private def listHDFSFolder(datasetPath:String):Future[Either[Error,List[(String,Boolean)]]] = {

    val queryString: Map[String, String] =Map("op"->"LISTSTATUS")

    webHDFSApiProxy.callHdfsService("GET",datasetPath,queryString,None) map{
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

    Logger.debug(s"setDatasetHDFSPermission datasetPath: $datasetPath")

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

    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString,None) map{
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
      info <- stepOverF( getDatasetInfo(datasetName) ); (datasetPath,owner,ownerOrg)=info

      j0 <- stepOverF( testUser(owner) )
      j1 <- stepOverF( testIfUserCanGivePermission(ownerOrg,groupName) )

      k <- stepOverF( deletePermissionIfPresent(datasetName, groupName, groupType) )

      a <- step( setDatasetHDFSPermission(datasetPath, true, createPermission) )
      b <- step( a, createImpalaGrant(datasetPath, groupName, groupType, permission) )

      c <- step( b, createSupersetTable(datasetPath, groupName))

      d <- step( c, addAclToCatalog(datasetName, groupName, groupType, permission) )

    } yield d

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

  private def createSupersetTable(datasetPath:String, groupName:String) = {

    val dbName = IntegrationService.toSupersetDS(groupName)
    val schemaName = Some(toTableName(datasetPath).split('.')(0))
    val tableName = toTableName(datasetPath).split('.').last

    if(groupName != OPEN_DATA_GROUP)
      integrationService.createSupersetTable(dbName, schemaName, tableName)
    else
      supersetApiClient.addTablesPermissionToRole(schemaName, tableName ,ConfigReader.suspersetOpenDataRole)

  }

  private def deleteSupersetTable(datasetPath:String, groupName:String) = {

    val dbName = IntegrationService.toSupersetDS(groupName)
    val schemaName = Some(toTableName(datasetPath).split('.')(0))
    val tableName = toTableName(datasetPath).split('.').last

    if(groupName != OPEN_DATA_GROUP)
      integrationService.deleteSupersetTable(dbName, schemaName, tableName)
    else
      supersetApiClient.removeTablesPermissionFromRole(schemaName, tableName ,ConfigReader.suspersetOpenDataRole)

  }

  private def testSupersetDeleteTable(datasetPath:String, groupName:String) = {

      val dbName = IntegrationService.toSupersetDS(groupName)
      val schemaName = Some(toTableName(datasetPath).split('.')(0))
      val tableName = toTableName(datasetPath).split('.').last

      if(groupName != OPEN_DATA_GROUP)
        integrationService.testSupersetTableDelete(dbName, schemaName, tableName)
      else
        Future.successful{Right(Success(Some("ok"),Some("ok")))}

  }

  private def deleteAclFromCatalog(datasetName:String, groupName:String, groupType:String ):Future[Either[Error,Success]] = {

    Logger.debug(s"deleteAclFromCatalog datasetName: $datasetName")

    evalInFuture0S( MongoService.removeAllACL(datasetName, groupName, groupType) )
  }


  private def revokeImpalaGrant(datasetPath:String, groupName:String, groupType:String):Future[Either[Error,Success]] = {

    Logger.debug(s"revokeImpalaGrant datasetPath: $datasetPath")

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

    webHDFSApiProxy.callHdfsService("PUT",datasetPath,queryString,None) map{
      case Right(r) => Right( Success(Some("HDFS ACL deleted"),Some("ok")) )
      case Left(l) => Left(Error(Option(0), Some(l.jsValue.toString()), None))
    }

  }

  private def hardDeletePermissionFromACL(datasetName:String, groupName:String, groupType:String) :Future[Either[ErrorWrapper,SuccessWrapper]] = {

    Logger.debug(s"hardDeletePermissionFromACL datasetName: $datasetName")

    val deletePermission:(String,Boolean)=>Future[Either[Error,Success]] = revokeHDFSPermission(_, _, groupName, groupType)

    val result = for {

      info <- stepOverF( getDatasetInfo(datasetName) ); (datasetPath,owner,ownerOrg)=info
      a <- step( setDatasetHDFSPermission(datasetPath, true, deletePermission) )

      b <- step(a, revokeImpalaGrant(datasetPath, groupName, groupType) )

      c <- step(b, deleteSupersetTable(datasetPath,groupName) )

      d <- step(c, deleteAclFromCatalog(datasetName, groupName, groupType) )

    } yield d


    result.value

  }


  def deletePermissionFromACL(datasetName:String, groupName:String, groupType:String) :Future[Either[Error,Success]] = {

    val result = for {

      info <- stepOverF( getDatasetInfo(datasetName) ) ; (datasetPath,owner,ownerOrg)=info

      a <- stepOverF( testUser(owner) )
      b <- stepOverF( testIfUserCanGivePermission(ownerOrg,groupName) )
      b1 <- stepOverF( testSupersetDeleteTable(datasetPath,groupName) )


      c <- EitherT( hardDeletePermissionFromACL(datasetName, groupName, groupType) )

    } yield c

    result.value map{
      case Right(r) =>Right(Success(Some("ACL deleted"),Some("ok")))
      case Left(l) => if( l.steps == 0 ) Left(l.error)
                      else throw new Exception( s"deletePermissionFromACL process issue: process steps=${l.steps}" )
    }
  }


  def getPermissions(datasetName:String) :Future[Either[Error,Option[Seq[AclPermission]]]] = {
    //Left if there are errors
    //Rignt(None) if acl are not founds
    def methodCall = MongoService.getACL(datasetName)
    evalInFuture0(methodCall).map{
      case Right(Some(json)) => json.validate[Seq[AclPermission]] match {
        case s: JsSuccess[Seq[AclPermission]] => Right(Some(s.get))
        case e: JsError => Left( Error(Option(0), Some(WebServiceUtil.getMessageFromJsError(e)), None) )
      }
      case Right(None) => Right(None)
      case Left(l) => Left(l)
    }
  }

  private def checkPermissions(datasetName:String,groupName:String,groupType:String):Future[Either[Error,Boolean]] = {

    // Left errors
    // Left(Error(None, None, None)) no acl

    getPermissions(datasetName).map{
                                      case Right(Some(r)) =>  if( r.exists(a => a.groupName.equals(groupName) && a.groupType.equals(groupType)) ) Right(true)
                                                              else Right(false)

                                      //if(r.isEmpty) Left(Error(None, None, None))
                                      // else Right{Success(Some(""), Some(""))}

                                      case Right(None) =>  Right(false)

                                      case Left(l) => Left(l)
                                    }
  }

  private def deletePermissionIfPresent(datasetName:String, groupName:String, groupType:String) :Future[Either[Error,Success]] = {

    logger.info(s"deletePermissionIfPresent datasetName: $datasetName, groupName: $groupName, groupType: $groupType")


    for{
      a <- checkPermissions(datasetName, groupName, groupType)
      b <- if(a.right.get) hardDeletePermissionFromACL(datasetName, groupName, groupType) map{
                                                                                                case Right(r) => Right(r.success)
                                                                                                case Left(l) => if( l.steps == 0 ) Left(l.error)
                                                                                                else throw new Exception( s"deletePermissionIfPresent process issue: process steps=${l.steps}" )
                                                                                              }
           else Future.successful( Right{Success(Some("Nothing todo"), Some("ok"))} )
    } yield b

    /*
    val out = checkPermissions(datasetName, groupName, groupType)


    out flatMap { case Right(true) => hardDeletePermissionFromACL(datasetName, groupName, groupType) map{
                                                                                                      case Right(r) => Right(r.success)
                                                                                                      case Left(l) => if( l.steps == 0 ) Left(l.error)
                                                                                                      else throw new Exception( s"deletePermissionIfPresent process issue: process steps=${l.steps}" )
                                                                                                      }
                  case Right(false) => Future.successful( Right{Success(Some("Nothing todo"), Some("ok"))} )
                  case Left(l) => Left(l)
                }*/

  }

  def toTableName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').drop(4).mkString(".")
  def toDatasetName(datasetPhisicalURI:String)=datasetPhisicalURI.split('/').last.split("_o_").last

}

