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
import ProcessHandler._

@Singleton
class IntegrationService @Inject()(apiClientIPA:ApiClientIPA, supersetApiClient: SupersetApiClient, ckanApiClient: CkanApiClient, grafanaApiClient:GrafanaApiClient, registrationService: RegistrationService,kyloApiClient:KyloApiClient){

  import play.api.libs.concurrent.Execution.Implicits._

  def createDafOrganization(dafOrg:DafOrg):Future[Either[Error,Success]] = {


    val groupCn = dafOrg.groupCn
    val predefinedOrgIpaUser = new IpaUser(groupCn,
                                          "predefined organization user",
                                          dafOrg.predefinedUserMail.getOrElse( toMail(groupCn) ),
                                          toUserName(groupCn),
                                          Option(Role.Editor.toString),
                                          Option(dafOrg.predefinedUserPwd),
                                          None,
                                          Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- step( apiClientIPA.createGroup(dafOrg.groupCn ) )
      b <- step( a, registrationService.createUser(predefinedOrgIpaUser,true) )
      c <- step( b, supersetApiClient.createDatabase(toSupersetDS(groupCn),predefinedOrgIpaUser.uid,dafOrg.predefinedUserPwd,dafOrg.supSetConnectedDbName) )
      d <- stepOver( c, kyloApiClient.createCategory(dafOrg.groupCn) )

      e <- step( c, ckanApiClient.createOrganizationAsAdmin(groupCn) )
      //f <- EitherT( grafanaApiClient.createOrganization(groupCn) ) TODO da riabilitare
      g <- step( e, addUserToOrganization(groupCn,predefinedOrgIpaUser.uid) )
      //g <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,toUserName(groupCn)) )
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


  }


  // only setup freeIPA and Superset
  def createDefaultDafOrganization(passwd:String):Future[Either[Error,Success]] = {

    val dafOrg = DafOrg(ConfigReader.defaultOrganization, passwd, "opendata", None)

    val groupCn = dafOrg.groupCn
    val defaultOrgIpaUser = IpaUser( dafOrg.groupCn,
      "default org default admin",
      toMail(groupCn),
      toUserName(groupCn),
      Option(Role.Admin.toString),
      Option(dafOrg.predefinedUserPwd),
      None,
      Option(Seq(dafOrg.groupCn)))


    val result = for {
      a <- EitherT( apiClientIPA.createGroup(dafOrg.groupCn) )
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

  }


  def deleteDafOrganization(groupCn:String):Future[Either[Error,Success]] = {

    val result = for {

      a <- stepOver( apiClientIPA.isEmptyGroup(groupCn) )
      dbId <- stepOverF( supersetApiClient.findDatabaseId(toSupersetDS(groupCn)) )
      b <- stepOver( supersetApiClient.checkDbTables(dbId) )
      c <- EitherT( hardDeleteDafOrganization(groupCn) )

    } yield c


    result.value.map{
      case Right(r) => Right( Success(Some("Organization created"), Some("ok")) )
      case Left(l) => if( l.steps == 0 )
                        Left(l.error)
                      else
                        throw new Exception( s"DeleteDafOrganization process issue: process steps=${l.steps}" )

    }
  }


  private def hardDeleteDafOrganization(groupCn:String):Future[Either[ErrorWrapper,SuccessWrapper]] = {

    val result = for {

      a <- step( apiClientIPA.deleteGroup(groupCn) )
      b <- step( a, apiClientIPA.deleteUser(toUserName(groupCn)) )

      userInfo <- stepOverF(a, supersetApiClient.findUser(toUserName(groupCn)) )
      c <- step( b, supersetApiClient.deleteUser(userInfo._1) )

      roleId <- stepOverF(c, supersetApiClient.findRoleId(toSupersetRole(groupCn)) )
      d <- stepOver( c, supersetApiClient.deleteRole(roleId) )

      dbId <- stepOverF(c, supersetApiClient.findDatabaseId(toSupersetDS(groupCn)) )
      e <- stepOver(c, supersetApiClient.deleteDatabase(dbId) )
      f <- step(c, clearSupersetPermissions(dbId, toSupersetDS(groupCn)) )


      g <- stepOver( f, ckanApiClient.deleteOrganization(groupCn) )
      h <- step( f, ckanApiClient.purgeOrganization(groupCn) )

      //i <- EitherT( grafanaApiClient.deleteOrganization(groupCn) ) TODO da riabilitare
    } yield h

    result.value
  }


  def addUserToOrganization(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    //predefinedUserId = ; b1 <- EitherT( apiClientIPA.addUsersToGroup(groupCn,UserList(Option(Seq(userName)))) )

    val result = for {
      user <-  EitherT( apiClientIPA.findUserByUid(userName) )
      //a0 <- EitherT( registrationService.testIfIsNotPredefinedUser(user) )// cannot remove predefined user
      a1<-  EitherT( registrationService.testIfUserBelongsToThisGroup(user,groupCn) )
      a2 <- EitherT( apiClientIPA.addUsersToGroup(groupCn,Seq(userName)) )
      supersetUserInfo <- EitherT( supersetApiClient.findUser(userName) )
      roleIds <- EitherT( supersetApiClient.findRoleIds(toSupersetRole(groupCn)::supersetUserInfo._2.toList:_*) )

      a <- EitherT( supersetApiClient.updateUser(user,supersetUserInfo._1,roleIds) )
      /*
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )
      */
      org <- EitherT( ckanApiClient.getOrganizationAsAdmin(groupCn) )
      c <- EitherT( ckanApiClient.putUserInOrganizationAsAdmin(userName,org) )

      //d <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,userName) ) TODO da riabilitare
    } yield c

    result.value
  }


  def removeUserFromOrganization(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    if( groupCn.equals(ConfigReader.defaultOrganization) )
      Future{ Left( Error(Option(1),Some("Cannot remove users from default organization"),None) ) }

    else{

      val result = for {
        user <-  EitherT( apiClientIPA.findUserByUid(userName) )
        a0 <- EitherT( registrationService.testIfIsNotPredefinedUser(user) )// cannot remove predefined user

        a1 <-  EitherT( apiClientIPA.removeUsersFromGroup(groupCn,Seq(userName)) )

        supersetUserInfo <- EitherT( supersetApiClient.findUser(userName) )
        roleNames = supersetUserInfo._2.toList.filter( p=>(!p.equals(toSupersetRole(groupCn))) ); roleIds <- EitherT( supersetApiClient.findRoleIds(roleNames:_*) )

        a <- EitherT( supersetApiClient.updateUser(user,supersetUserInfo._1,roleIds) )
        /*
        a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
        b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )*/

        org <- EitherT( ckanApiClient.getOrganizationAsAdmin(groupCn) )
        c <- EitherT( ckanApiClient.removeUserInOrganizationAsAdmin(userName,org) )

        //d <- EitherT( grafanaApiClient ) TODO da gestire
      } yield c

      result.value

    }

  }


  def createSupersetTable(dbName:String, schema:Option[String], tableName:String):Future[Either[Error,Success]] = {

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
  def toEditorGroupName(groupCn:String)=s"$groupCn-edit"
  def toViewerGroupName(groupCn:String)=s"$groupCn-view"
  def toSupersetDS(groupCn:String)=s"$groupCn-db"
  def toSupersetRole(groupCn:String)=s"datarole-$groupCn-db"
  def toUserName(groupCn:String)=groupCn+"_default_admin"
  def toMail(groupCn:String)=s"$groupCn@default.it"
}
