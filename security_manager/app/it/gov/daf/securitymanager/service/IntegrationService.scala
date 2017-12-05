package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import it.gov.daf.sso.ApiClientIPA
import security_manager.yaml.{DafOrg, Error, IpaUser, Success}
import scala.concurrent.Future
import cats.implicits._

@Singleton
class IntegrationService @Inject()(apiClientIPA:ApiClientIPA,supersetApiClient: SupersetApiClient,ckanApiClient: CkanApiClient){

  import scala.concurrent.ExecutionContext.Implicits._

  private def toDataSource(groupCn:String)=s"$groupCn-db"
  private def toRoleName(groupCn:String)=s"datarole-$groupCn-db"
  private def toUserName(groupCn:String)=s"$groupCn-default-admin"
  private def toMail(groupCn:String)=s"$groupCn@default.it"


  def createDafOrganization(dafOrg:DafOrg):Future[Either[Error,Success]] = {

    val groupCn = dafOrg.groupCn
    val defaultOrgIpaUser = new IpaUser(  dafOrg.groupCn,
                                          "default org admin",
                                          toMail(groupCn),
                                          toUserName(groupCn),
                                          Option(Role.Admin.toString),
                                          Option(dafOrg.defaultUserPwd),
                                          Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- EitherT( apiClientIPA.createGroup(dafOrg.groupCn) )
      b <- EitherT( apiClientIPA.createUser(defaultOrgIpaUser) )
      //b <- EitherT( apiClientIPA.addUsersToGroup(dafOrg.groupCn, UserList(Option(Seq(defaultOrgIpaUser.uid)))) )
      c <- EitherT( supersetApiClient.createDatabase(toDataSource(groupCn),toUserName(groupCn),dafOrg.defaultUserPwd,dafOrg.supSetConnectedDbName) )
      orgAdminRoleId <- EitherT( supersetApiClient.findRoleId(ConfigReader.suspersetOrgAdminRole) )
      dataOrgRoleId <- EitherT( supersetApiClient.findRoleId(toRoleName(groupCn)) )
      d <- EitherT( supersetApiClient.createUserWithRoles(defaultOrgIpaUser,orgAdminRoleId,dataOrgRoleId) )
      e <- EitherT( ckanApiClient.createOrganization(toUserName(groupCn),dafOrg.defaultUserPwd,dafOrg.groupCn) )
    } yield e

    result.value

    /* per cancellare automatcamente tutto in caso di errore
    result.value.flatMap{

      case Right(r) => result.value
      case Left(l) => {
        deleteDafOrganization(dafOrg.groupCn)
        result.value
      }

    }*/

  }


  def deleteDafOrganization(groupCn:String):Future[Either[Error,Success]] = {

    val result = for {
      a <- EitherT( apiClientIPA.deleteGroup(groupCn) )
      b <- EitherT( apiClientIPA.deleteUser(toUserName(groupCn)) )

      dbId <- EitherT( supersetApiClient.findDatabaseId(toDataSource(groupCn)) )
      c <- EitherT( supersetApiClient.deleteDatabase(dbId) )

      roleId <- EitherT( supersetApiClient.findRoleId(toRoleName(groupCn)) )
      d <- EitherT( supersetApiClient.deleteRole(roleId) )

      userInfo <- EitherT( supersetApiClient.findUser(toUserName(groupCn)) )
      e <- EitherT( supersetApiClient.deleteUser(userInfo._1) )

      f <- EitherT( ckanApiClient.deleteOrganization(groupCn) )
      g <- EitherT( ckanApiClient.purgeOrganization(groupCn) )
    } yield g

    result.value
  }


  def addUserToOrganization(groupCn:String, userName:String, loggedUserName:String ):Future[Either[Error,Success]] = {

    val result = for {
      user <-  EitherT( apiClientIPA.showUser(userName) )
      supersetUserInfo <- EitherT( supersetApiClient.findUser(userName) )
      roleIds <- EitherT( supersetApiClient.findRoleIds(toRoleName(groupCn)::supersetUserInfo._2.toList:_*) )
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )

      org <- EitherT( ckanApiClient.getOrganization(loggedUserName,groupCn) )
      c <- EitherT( ckanApiClient.putUserInOrganization(loggedUserName,userName,org) )
    } yield c

    result.value
  }


  def addNewUserToDefultOrganization(ipaUser:IpaUser):Future[Either[Error,Success]] = {

    val result = for {
      roleIds <- EitherT( supersetApiClient.findRoleIds(ConfigReader.suspersetOrgAdminRole,toRoleName(ConfigReader.defaultOrganization)) )
      a <- EitherT( supersetApiClient.createUserWithRoles(ipaUser,roleIds:_*) )
    } yield a

    result.value
  }



}
