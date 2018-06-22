package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.sso._
import security_manager.yaml.{DafGroup, Error, IpaUser, Success}
import ProcessHandler._

import scala.concurrent.Future
import cats.implicits._
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import IntegrationService._
import it.gov.daf.common.sso.common.{Admin, Editor, Viewer}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.Try

@Singleton
class IntegrationService @Inject()(apiClientIPA:ApiClientIPA, supersetApiClient: SupersetApiClient, ckanApiClient: CkanApiClient, grafanaApiClient:GrafanaApiClient, registrationService: RegistrationService,kyloApiClient:KyloApiClient, impalaService:ImpalaService){

  private val logger = Logger(this.getClass.getName)

  def createDafOrganization(dafOrg:DafGroup):Future[Either[Error,Success]] = {

    logger.info("createDafOrganization")

    val groupCn = dafOrg.groupCn
    val predefinedOrgIpaUser =     IpaUser(groupCn,
                                          "reference organization user",
                                          toOrgMail(groupCn),
                                          toRefOrgUserName(groupCn),
                                          None,
                                          Option(Seq(Viewer.toString+groupCn)),
                                          Option(dafOrg.predefinedUserPwd),
                                          None,
                                          Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- step( apiClientIPA.createGroup(Organization(dafOrg.groupCn),None) )

      a1 <- step( a, apiClientIPA.createGroup(RoleGroup(Admin.toString+groupCn),None) )
      a2 <- step( a1, apiClientIPA.createGroup(RoleGroup(Editor.toString+groupCn),None) )
      a3 <- step( a2, apiClientIPA.createGroup(RoleGroup(Viewer.toString+groupCn),None) )

      a4 <- step( a3, evalInFuture0S(impalaService.createRole(groupCn,false)) )

      b <- step( a4, registrationService.checkMailNcreateUser(predefinedOrgIpaUser,true) )
      c <- step( b, supersetApiClient.createDatabase(toSupersetDS(groupCn),predefinedOrgIpaUser.uid,dafOrg.predefinedUserPwd,dafOrg.supSetConnectedDbName) )
      d <- stepOver( c, kyloApiClient.createCategory(dafOrg.groupCn) )

      //e <- step( c, ckanApiClient.createOrganizationAsAdmin(groupCn) ) non si usa + ckan
      e <- step( c, ckanApiClient.createOrganizationInGeoCkanAsAdmin(groupCn) )
      //f <- EitherT( grafanaApiClient.createOrganization(groupCn) ) TODO da riabilitare
      g <- stepOver( e, addUserToOrganization(groupCn,predefinedOrgIpaUser.uid) )
      //g <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,toUserName(groupCn)) )
    } yield g


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


  }

  private def hardDeleteDafOrganization(groupCn:String):Future[Either[ErrorWrapper,SuccessWrapper]] = {

    logger.info("hardDeleteDafOrganization")

    val result = for {

      a <- step( apiClientIPA.deleteGroup(groupCn) )
      a1 <- step( a, apiClientIPA.deleteGroup(Admin.toString+groupCn) )
      a2 <- step( a1, apiClientIPA.deleteGroup(Editor.toString+groupCn) )
      a3 <- step( a2, apiClientIPA.deleteGroup(Viewer.toString+groupCn) )

      a4 <- step( a3, evalInFuture0S(impalaService.deleteRole(groupCn,false)))

      c <- step( a4, registrationService.callHardDeleteUser(toRefOrgUserName(groupCn)) )


        /*
      b <- stepOver( a4, apiClientIPA.deleteUser(toRefOrgUserName(groupCn)) )
      userInfo <- stepOverF( b, supersetApiClient.findUser(toRefOrgUserName(groupCn)) )
      c <- step( b, supersetApiClient.deleteUser(userInfo._1) )
      */

      roleId <- stepOverF( c, supersetApiClient.findRoleId(toSupersetRole(groupCn)) )
      d <- stepOver( c, supersetApiClient.deleteRole(roleId) )

      dbId <- stepOverF(c, supersetApiClient.findDatabaseId(toSupersetDS(groupCn)) )
      e <- step( c, supersetApiClient.deleteDatabase(dbId) )
      //f <- stepOver( c, Try{clearSupersetPermissions(dbId, toSupersetDS(groupCn))} ) TODO to re-enable when Superset bug is resolved


      //g <- stepOver( e, ckanApiClient.deleteOrganization(groupCn) )
      //h <- step( g, ckanApiClient.purgeOrganization(groupCn) )

      g <- stepOver( e, ckanApiClient.deleteOrganizationInGeoCkan(groupCn) )
      h <- step( g, ckanApiClient.purgeOrganizationInGeoCkan(groupCn) )

      //i <- EitherT( grafanaApiClient.deleteOrganization(groupCn) ) TODO re-enable when Grafana is integrated
    } yield h

    result.value
  }

  def deleteDafOrganization(groupCn:String):Future[Either[Error,Success]] = {

    logger.info("deleteDafOrganization")

    val result = for {

      a <- stepOver( apiClientIPA.testGroupForDeletion(Organization(groupCn)) )
      dbId <- stepOverF( supersetApiClient.findDatabaseId(toSupersetDS(groupCn)) )
      b <- stepOver( supersetApiClient.checkDbTables(dbId) )
      c <- EitherT( hardDeleteDafOrganization(groupCn) )

    } yield c


    result.value.map{
      case Right(r) => Right( Success(Some("Organization deleted"), Some("ok")) )
      case Left(l) => if( l.steps == 0 )
                        Left(l.error)
                      else
                        throw new Exception( s"DeleteDafOrganization process issue: process steps=${l.steps}" )

    }
  }


  def createDafWorkgroup(dafWrk:DafGroup, orgName:String):Future[Either[Error,Success]] = {

    logger.info("createDafWorkgroup")

    val wrkName = dafWrk.groupCn
    val predefinedWrkIpaUser =     IpaUser(wrkName,
      "reference workgroup user",
      toWrkMail(wrkName),
      toRefWrkUserName(wrkName),
      None,
      Option(Seq(Viewer.toString+orgName)),
      Option(dafWrk.predefinedUserPwd),
      None,
      Option(Seq(dafWrk.groupCn)))


    val result = for {
      a <- step( apiClientIPA.createGroup(WorkGroup(wrkName),Some(Organization(orgName))) )

      a4 <- step( a, evalInFuture0S(impalaService.createRole(wrkName,false)) )

      b <- step( a4, registrationService.checkMailNcreateUser(predefinedWrkIpaUser,true) )
      c <- step( b, supersetApiClient.createDatabase(toSupersetDS(wrkName),predefinedWrkIpaUser.uid,dafWrk.predefinedUserPwd,dafWrk.supSetConnectedDbName) )

      //f <- EitherT( grafanaApiClient.createOrganization(groupCn) ) TODO da riabilitare
      d <- stepOver( c, addUserToOrganization(orgName,predefinedWrkIpaUser.uid) )
      g <- stepOver( d, addUserToWorkgroup(wrkName,predefinedWrkIpaUser.uid) )
      //g <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,toUserName(groupCn)) )
    } yield g


    result.value.map{
      case Right(r) => Right( Success(Some("Workgroup created"), Some("ok")) )
      case Left(l) => println("stepss"+l.steps);if( l.steps !=0 ) {
        hardDeleteDafWorkgroup(wrkName).onSuccess { case e =>

          val steps = e.fold(ll=>ll.steps,rr=>rr.steps)
          if( l.steps != steps)
            throw new Exception( s"createDafWorkgroup rollback issue: process steps=${l.steps} rollback steps=$steps" )

        }

      }
        Left(l.error)
    }


  }

  private def hardDeleteDafWorkgroup(groupCn:String):Future[Either[ErrorWrapper,SuccessWrapper]] = {

    logger.info("hardDeleteDafWorkgroup")

    val result = for {

      a <- step( apiClientIPA.deleteGroup(groupCn) )

      a1 <- step( a, evalInFuture0S(impalaService.deleteRole(groupCn,false)) )

      b <- step( a1, registrationService.callHardDeleteUser(toRefWrkUserName(groupCn)) )

      /*
      b <- stepOver( a4, apiClientIPA.deleteUser(toRefOrgUserName(groupCn)) )
      userInfo <- stepOverF( b, supersetApiClient.findUser(toRefOrgUserName(groupCn)) )
      c <- step( b, supersetApiClient.deleteUser(userInfo._1) )
      */

      roleId <- stepOverF( b, supersetApiClient.findRoleId(toSupersetRole(groupCn)) )
      d <- stepOver( b, supersetApiClient.deleteRole(roleId) )

      dbId <- stepOverF(d, supersetApiClient.findDatabaseId(toSupersetDS(groupCn)) )
      e <- step( d, supersetApiClient.deleteDatabase(dbId) )
      //f <- stepOver( c, Try{clearSupersetPermissions(dbId, toSupersetDS(groupCn))} ) TODO to re-enable when Superset bug is resolved
      //i <- EitherT( grafanaApiClient.deleteOrganization(groupCn) ) TODO re-enable when Grafana is integrated
    } yield e

    result.value
  }


  def deleteDafWorkgroup(groupCn:String):Future[Either[Error,Success]] = {

    logger.info("deleteDafWorkgroup")

    val result = for {
      // TODO test user org role
      a <- stepOver( apiClientIPA.testGroupForDeletion(WorkGroup(groupCn)) )
      dbId <- stepOverF( supersetApiClient.findDatabaseId(toSupersetDS(groupCn)) )
      b <- stepOver( supersetApiClient.checkDbTables(dbId) )
      c <- EitherT( hardDeleteDafWorkgroup(groupCn) )

    } yield c

    result.value.map{
      case Right(r) => Right( Success(Some("Workgroup deleted"), Some("ok")) )
      case Left(l) => if( l.steps == 0 )
        Left(l.error)
      else
        throw new Exception( s"deleteDafWorkgroup process issue: process steps=${l.steps}" )

    }

  }

  // only setup freeIPA and Superset
  /*
  def createDefaultDafOrganization(passwd:String):Future[Either[Error,Success]] = {

    val dafOrg = DafOrg(ConfigReader.defaultOrganization, passwd, "opendata")

    val groupCn = dafOrg.groupCn
    val defaultOrgIpaUser = IpaUser( dafOrg.groupCn,
      "default org default admin",
      toMail(groupCn),
      toRefUserName(groupCn),
      Option(Role.Admin.toString),
      Option(dafOrg.predefinedUserPwd),
      None,
      Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- EitherT( apiClientIPA.createGroup(Organization(dafOrg.groupCn)) )
      b <- EitherT( registrationService.createDefaultUser(defaultOrgIpaUser) )
      c <- EitherT( supersetApiClient.createDatabase(toSupersetDS(groupCn),defaultOrgIpaUser.uid,dafOrg.predefinedUserPwd,dafOrg.supSetConnectedDbName) )
      //e <- EitherT( ckanApiClient.createOrganizationAsAdmin(groupCn) )
      //f <- EitherT( grafanaApiClient.createOrganization(groupCn) )
      //g <- EitherT( addUserToOrganizationAsAdmin(groupCn,defaultOrgIpaUser.uid) )

    } yield c


    result.value.map{
      case Right(r) => Right( Success(Some("Organization created"), Some("ok")) )
      case Left(l) => Left(l)
    }

  }*/


  def addUserToOrganization(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    logger.info("addUserToOrganization")

    val orgViewerRoleName= RoleGroup(Viewer.toString+groupCn).toString

    val result = for {
      a <- stepOver( apiClientIPA.testIfIsOrganization(groupCn) )
      user <- stepOverF( apiClientIPA.findUser(Left(userName)) )
      a1<-  stepOver( registrationService.raiseErrorIfUserAlreadyBelongsToThisGroup(user,groupCn) )
      a2 <- step( apiClientIPA.addMemberToGroups(Some(Seq(groupCn,orgViewerRoleName)),User(userName)) )
      supersetUserInfo <- stepOverF( a2, supersetApiClient.findUser(userName) )
      roleIds <- stepOverF( a2, supersetApiClient.findRoleIds(toSupersetRole(groupCn)::supersetUserInfo._2.toList:_*) )

      a <- step( a2, supersetApiClient.updateUser(user,supersetUserInfo._1,roleIds) )
      /*
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )
      */
      org <- stepOverF( a, ckanApiClient.getOrganizationAsAdminInGeoCkan(groupCn) )
      c <- step( a, ckanApiClient.putUserInOrganizationAsAdminInGeoCkan(userName,org) )

      //d <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,userName) ) TODO re-enable when Grafana is integrated
    } yield c

    result.value.map{
      case Right(r) => Right( Success(Some("Added user to "+groupCn), Some("ok")) )
      case Left(l) => if( l.steps !=0 ) {
        hardRemoveUserFromOrganization(groupCn,userName).onSuccess { case e =>

          val steps = e.fold(ll=>ll.steps,rr=>rr.steps)
          if( l.steps != steps)
            throw new Exception( s"AddUserToOrgasnization rollback issue: process steps=${l.steps} rollback steps=$steps" )

        }

      }
        Left(l.error)
    }
  }

  private def hardRemoveUserFromOrganization(groupCn:String, userName:String):Future[Either[ErrorWrapper,SuccessWrapper]] = {

    logger.info("hardRemoveUserFromOrganization")

    val orgGroups= Some(Seq(groupCn,
                            RoleGroup(Viewer.toString+groupCn).toString,
                            RoleGroup(Editor.toString+groupCn).toString,
                            RoleGroup(Admin.toString+groupCn).toString))

    val result = for {
      user <- stepOverF( apiClientIPA.findUser(Left(userName)) )

      a1 <- step( apiClientIPA.removeMemberFromGroups(orgGroups,User(userName)) )

      supersetUserInfo <- stepOverF(a1, supersetApiClient.findUser(userName) )
      roleNames = supersetUserInfo._2.toList.filter(p => !p.equals(toSupersetRole(groupCn))); roleIds <- stepOverF(a1, supersetApiClient.findRoleIds(roleNames: _*) )

      a <- step(a1, supersetApiClient.updateUser(user, supersetUserInfo._1, roleIds) )
      /*
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )*/

      org <- stepOverF(a, ckanApiClient.getOrganizationAsAdminInGeoCkan(groupCn) )
      c <- step(a, ckanApiClient.removeUserInOrganizationAsAdminInGeoCkan(userName, org) )

      //d <- EitherT( grafanaApiClient ) TODO re-enable when Grafana is integrated (review)
    } yield c

    result.value

  }

  def removeUserFromOrganization(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    logger.info("removeUserFromOrganization")

    val result = for {
      //user <-  stepOverF( Try{apiClientIPA.findUserByUid(userName)} )
      a0 <- stepOver( apiClientIPA.testIfIsOrganization(groupCn) )
      a <- stepOver( registrationService.raiseErrorIfIsReferenceUser(userName) )// cannot remove predefined user

      b <-  EitherT( hardRemoveUserFromOrganization(groupCn,userName) )

    } yield b

    result.value.map{
      case Right(r) => Right( Success(Some(s"User $userName deleted from organization $groupCn"), Some("ok")) )
      case Left(l) => if( l.steps == 0 )
        Left(l.error)
      else
        throw new Exception( s"removeUserFromOrganization process issue: process steps=${l.steps}" )

    }

  }

  def addUserToWorkgroup(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    logger.info("addUserToWorkgroup")

    val result = for {
      parentOrg <- stepOverF( apiClientIPA.getWorkgroupOrganization(groupCn) )
      user <- stepOverF( apiClientIPA.findUser(Left(userName)) )
      a1<- stepOver( registrationService.raiseErrorIfUserDoesNotBelongToThisGroup(user,parentOrg) )
      a2 <- step( apiClientIPA.addMembersToGroup(groupCn,User(userName)) )
      supersetUserInfo <- stepOverF( a2, supersetApiClient.findUser(userName) )
      roleIds <- stepOverF( a2, supersetApiClient.findRoleIds(toSupersetRole(groupCn)::supersetUserInfo._2.toList:_*) )

      a <- step( a2, supersetApiClient.updateUser(user,supersetUserInfo._1,roleIds) )
      /*
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )
      */

      //d <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,userName) ) TODO re-enable when Grafana is integrated
    } yield a

    result.value.map{
      case Right(r) => Right( Success(Some("Added user to "+groupCn), Some("ok")) )
      case Left(l) => if( l.steps !=0 ) {
        hardRemoveUserFromWorkgroup(groupCn,userName).onSuccess { case e =>

          val steps = e.fold(ll=>ll.steps,rr=>rr.steps)
          if( l.steps != steps)
            throw new Exception( s"addUserToWorkgroup rollback issue: process steps=${l.steps} rollback steps=$steps" )

        }

      }
        Left(l.error)
    }
  }

  private def hardRemoveUserFromWorkgroup(groupCn:String, userName:String):Future[Either[ErrorWrapper,SuccessWrapper]] = {

    logger.info("hardRemoveUserFromWorkgroup")

    val result = for {
      user <- stepOverF( apiClientIPA.findUser(Left(userName)) )

      a1 <- step( apiClientIPA.removeMembersFromGroup(groupCn, User(userName)) )

      supersetUserInfo <- stepOverF( a1, supersetApiClient.findUser(userName) )
      roleNames = supersetUserInfo._2.toList.filter(p => !p.equals(toSupersetRole(groupCn))); roleIds <- stepOverF(a1, supersetApiClient.findRoleIds(roleNames: _*) )

      a <- step(a1, supersetApiClient.updateUser(user, supersetUserInfo._1, roleIds) )
      /*
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )*/

      //d <- EitherT( grafanaApiClient ) TODO re-enable when Grafana is integrated (review)
    } yield a

    result.value

  }

  def removeUserFromWorkgroup(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    logger.info("removeUserFromWorkgroup")

    val result = for {
      //user <-  stepOverF( Try{apiClientIPA.findUserByUid(userName)} )
      a0 <- stepOver( apiClientIPA.testIfIsWorkgroup(groupCn) )
      a <- stepOver( registrationService.raiseErrorIfIsReferenceUser(userName) )// cannot remove predefined user

      b <-  EitherT( hardRemoveUserFromWorkgroup(groupCn,userName) )

    } yield b

    result.value.map{
      case Right(r) => Right( Success(Some(s"User $userName deleted from workgroup $groupCn"), Some("ok")) )
      case Left(l) => if( l.steps == 0 )
        Left(l.error)
      else
        throw new Exception( s"removeUserFromWorkgroup process issue: process steps=${l.steps}" )
    }


  }


  def createSupersetTable(dbName:String, schema:Option[String], tableName:String):Future[Either[Error,Success]] = {

    logger.info("createSupersetTable")

    val result = for {
      dbId <-  EitherT( supersetApiClient.findDatabaseId(dbName) )
      a <-  EitherT( supersetApiClient.createTable(dbId,schema,tableName) )
      b <- EitherT( supersetApiClient.checkTable(dbId,schema,tableName) )
    } yield b


    result.value.map{
      case Right(r) => Right( Success(Some("Table created"), Some("ok")) )
      case Left(l) => Left(l)
    }

  }

  def getSupersetOrgTables(orgName:String):Future[Either[Error,Seq[String]]] = {

    logger.info("getSupersetOrgTables")

    val result = for {
      dbId <-  EitherT( supersetApiClient.findDatabaseId(toSupersetDS(orgName)) )
      a <-  EitherT( supersetApiClient.findDbTables(dbId) )
    } yield a

    result.value

  }

  private def clearSupersetPermissions(dbId:Long,dbName:String):Future[Either[Error,Success]] = {

    val permName = s"[$dbName].(id:$dbId)"
    val result = for {
      viewId <-  EitherT( supersetApiClient.findViewId(permName) )
      permViewIds <- EitherT( supersetApiClient.findPermissionViewIds(viewId) )
      a <- EitherT( supersetApiClient.deletePermissionsViews(permViewIds) )
      b <- EitherT( supersetApiClient.deleteView(viewId) )
    } yield b

    result.value
  }


}


object IntegrationService {
  //def toEditorGroupName(groupCn:String)=s"$groupCn-edit"
  //def toViewerGroupName(groupCn:String)=s"$groupCn-view"
  def toSupersetDS(groupCn:String)=s"$groupCn-db"
  def toSupersetRole(groupCn:String)=s"datarole-$groupCn-db"
  def toRefOrgUserName(groupCn:String)=groupCn+ORG_REF_USER_POSTFIX
  def toRefWrkUserName(groupCn:String)=groupCn+WRK_REF_USER_POSTFIX
  def toOrgMail(groupCn:String)=s"$groupCn@ref.usr.org.it"
  def toWrkMail(groupCn:String)=s"$groupCn@ref.usr.wrk.it"
}
