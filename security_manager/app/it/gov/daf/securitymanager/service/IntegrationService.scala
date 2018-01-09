package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.authentication.Role
import it.gov.daf.sso.ApiClientIPA
import security_manager.yaml.{DafOrg, Error, IpaUser, Success}

import scala.concurrent.Future
import cats.implicits._
import IntegrationService._
import it.gov.daf.securitymanager.service.utilities.ConfigReader

@Singleton
class IntegrationService @Inject()(apiClientIPA:ApiClientIPA, supersetApiClient: SupersetApiClient, ckanApiClient: CkanApiClient, grafanaApiClient:GrafanaApiClient, registrationService: RegistrationService){

  import scala.concurrent.ExecutionContext.Implicits._

  def createDafOrganization(dafOrg:DafOrg):Future[Either[Error,Success]] = {

    val groupCn = dafOrg.groupCn
    val defaultOrgIpaUser = new IpaUser(  groupCn,
                                          "default org admin",
                                          toMail(groupCn),
                                          toUserName(groupCn),
                                          Option(Role.Admin.toString),
                                          Option(dafOrg.defaultUserPwd),
                                          Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- EitherT( apiClientIPA.createGroup(dafOrg.groupCn) )
      b <- EitherT( registrationService.createUser(defaultOrgIpaUser) )
      c <- EitherT( supersetApiClient.createDatabase(toDataSource(groupCn),defaultOrgIpaUser.uid,dafOrg.defaultUserPwd,dafOrg.supSetConnectedDbName) )
      /*
      orgAdminRoleId <- EitherT( supersetApiClient.findRoleId(ConfigReader.suspersetOrgAdminRole) )
      dataOrgRoleId <- EitherT( supersetApiClient.findRoleId(toRoleName(groupCn)) )
      d <- EitherT( supersetApiClient.createUserWithRoles(defaultOrgIpaUser,orgAdminRoleId,dataOrgRoleId) )
      */
      e <- EitherT( ckanApiClient.createOrganizationAsAdmin(groupCn) )
      //f <- EitherT( grafanaApiClient.createOrganization(groupCn) ) TODO da riabilitare
      g <- EitherT( addUserToOrganizationAsAdmin(groupCn,defaultOrgIpaUser.uid) )
      //g <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,toUserName(groupCn)) )
    } yield g


    result.value.map{
      case Right(r) => Right( Success(Some("Organization created"), Some("ok")) )
      case Left(l) => Left(l)
    }

    /* per cancellare automatcamente tutto in caso di errore
    result.value.flatMap{

      case Right(r) => result.value
      case Left(l) => {
        deleteDafOrganization(dafOrg.groupCn)
        result.value
      }
    }*/

  }

  // only setup freeIPA and Superset
  def createDefaultDafOrganization():Future[Either[Error,Success]] = {

    val dafOrg = DafOrg(ConfigReader.defaultOrganization,"defaultusrpwd!","opendata")

    val groupCn = dafOrg.groupCn
    val defaultOrgIpaUser = IpaUser( dafOrg.groupCn,
      "default org default admin",
      toMail(groupCn),
      toUserName(groupCn),
      Option(Role.Admin.toString),
      Option(dafOrg.defaultUserPwd),
      Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- EitherT( apiClientIPA.createGroup(dafOrg.groupCn) )
      b <- EitherT( registrationService.createDefaultUser(defaultOrgIpaUser) )
      c <- EitherT( supersetApiClient.createDatabase(toDataSource(groupCn),defaultOrgIpaUser.uid,dafOrg.defaultUserPwd,dafOrg.supSetConnectedDbName) )
      //e <- EitherT( ckanApiClient.createOrganizationAsAdmin(groupCn) )
      //f <- EitherT( grafanaApiClient.createOrganization(groupCn) )
      //g <- EitherT( addUserToOrganizationAsAdmin(groupCn,defaultOrgIpaUser.uid) )

    } yield c


    result.value.map{
      case Right(r) => Right( Success(Some("Organization created"), Some("ok")) )
      case Left(l) => Left(l)
    }


  }


  def deleteDafOrganization(groupCn:String):Future[Either[Error,Success]] = {

    val result = for {

      dbId <- EitherT( supersetApiClient.findDatabaseId(toDataSource(groupCn)) )
      a1 <- EitherT( supersetApiClient.checkDbTables(dbId) )

      a <- EitherT( apiClientIPA.deleteGroup(groupCn) )
      b <- EitherT( apiClientIPA.deleteUser(toUserName(groupCn)) )

      userInfo <- EitherT( supersetApiClient.findUser(toUserName(groupCn)) )
      c <- EitherT( supersetApiClient.deleteUser(userInfo._1) )

      roleId <- EitherT( supersetApiClient.findRoleId(toRoleName(groupCn)) )
      d <- EitherT( supersetApiClient.deleteRole(roleId) )

      //dbId <- EitherT( supersetApiClient.findDatabaseId(toDataSource(groupCn)) )
      e <- EitherT( supersetApiClient.deleteDatabase(dbId) )
      f <- EitherT( clearSupersetPermissions(dbId, toDataSource(groupCn)) )


      g <- EitherT( ckanApiClient.deleteOrganization(groupCn) )
      h <- EitherT( ckanApiClient.purgeOrganization(groupCn) )

      //i <- EitherT( grafanaApiClient.deleteOrganization(groupCn) ) TODO da riabilitare
    } yield h

    result.value
  }


  def addUserToOrganization(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    val result = for {
      user <-  EitherT( apiClientIPA.showUser(userName) )
      supersetUserInfo <- EitherT( supersetApiClient.findUser(userName) )
      roleIds <- EitherT( supersetApiClient.findRoleIds(toRoleName(groupCn)::supersetUserInfo._2.toList:_*) )
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )

      org <- EitherT( ckanApiClient.getOrganizationAsAdmin(groupCn) )
      c <- EitherT( ckanApiClient.putUserInOrganizationAsAdmin(userName,org) )

      //d <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,userName) ) TODO da riabilitare
    } yield c

    result.value
  }

  def addUserToOrganizationAsAdmin(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    val result = for {
      user <-  EitherT( apiClientIPA.showUser(userName) )
      supersetUserInfo <- EitherT( supersetApiClient.findUser(userName) )
      roleIds <- EitherT( supersetApiClient.findRoleIds(toRoleName(groupCn)::supersetUserInfo._2.toList:_*) )
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )

      org <- EitherT( ckanApiClient.getOrganizationAsAdmin(groupCn) )
      c <- EitherT( ckanApiClient.putUserInOrganizationAsAdmin(userName,org) )

      //d <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,userName) ) TODO da riabilitare
    } yield c

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
  def toDataSource(groupCn:String)=s"$groupCn-db"
  def toRoleName(groupCn:String)=s"datarole-$groupCn-db"
  def toUserName(groupCn:String)=groupCn+"_default_admin"
  def toMail(groupCn:String)=s"$groupCn@default.it"
}
