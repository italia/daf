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

import scala.util.Try

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
      a <- step( Try{apiClientIPA.createGroup(dafOrg.groupCn)} )
      b <- step( a, Try{registrationService.checkMailNcreateUser(predefinedOrgIpaUser,true)} )
      c <- step( b, Try{supersetApiClient.createDatabase(toSupersetDS(groupCn),predefinedOrgIpaUser.uid,dafOrg.predefinedUserPwd,dafOrg.supSetConnectedDbName)} )
      d <- stepOver( c, Try{kyloApiClient.createCategory(dafOrg.groupCn)} )

      e <- step( c, Try{ckanApiClient.createOrganizationAsAdmin(groupCn)} )
      //f <- EitherT( grafanaApiClient.createOrganization(groupCn) ) TODO da riabilitare
      g <- stepOver( e, Try{addUserToOrganization(groupCn,predefinedOrgIpaUser.uid)} )
      //g <- EitherT( grafanaApiClient.addUserInOrganization(groupCn,toUserName(groupCn)) )
    } yield g

    // 4 step

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

    val result = for {

      a <- step( Try{apiClientIPA.deleteGroup(groupCn)} )
      b <- stepOver( a, Try{apiClientIPA.deleteUser(toUserName(groupCn))} )

      userInfo <- stepOverF( a, Try{supersetApiClient.findUser(toUserName(groupCn))} )
      c <- step( b, Try{supersetApiClient.deleteUser(userInfo._1)} )

      roleId <- stepOverF( c, Try{supersetApiClient.findRoleId(toSupersetRole(groupCn))} )
      d <- stepOver( c, Try{supersetApiClient.deleteRole(roleId)} )

      dbId <- stepOverF(c, Try{supersetApiClient.findDatabaseId(toSupersetDS(groupCn))} )
      e <- step( c, Try{supersetApiClient.deleteDatabase(dbId)} )
      //f <- stepOver( c, Try{clearSupersetPermissions(dbId, toSupersetDS(groupCn))} ) TODO to re-enable when Superset bug is resolved


      g <- stepOver( e, Try{ckanApiClient.deleteOrganization(groupCn)} )
      h <- step( g, Try{ckanApiClient.purgeOrganization(groupCn)} )

      //i <- EitherT( grafanaApiClient.deleteOrganization(groupCn) ) TODO re-enable when Grafana is integrated
    } yield h

    result.value
  }

  def deleteDafOrganization(groupCn:String):Future[Either[Error,Success]] = {

    val result = for {

      a <- stepOver( Try{apiClientIPA.isEmptyGroup(groupCn)} )
      dbId <- stepOverF( Try{supersetApiClient.findDatabaseId(toSupersetDS(groupCn))} )
      b <- stepOver( Try{supersetApiClient.checkDbTables(dbId)} )
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


  def addUserToOrganization(groupCn:String, userName:String):Future[Either[Error,Success]] = {


    val result = for {
      user <-  stepOverF( Try{apiClientIPA.findUserByUid(userName)} )
      a1<-  stepOver( Try{registrationService.testIfUserBelongsToThisGroup(user,groupCn)} )
      a2 <- step( Try{apiClientIPA.addUsersToGroup(groupCn,Seq(userName))} )
      supersetUserInfo <- stepOverF( Try{supersetApiClient.findUser(userName)} )
      roleIds <- stepOverF( Try{supersetApiClient.findRoleIds(toSupersetRole(groupCn)::supersetUserInfo._2.toList:_*)} )

      a <- step( a2, Try{supersetApiClient.updateUser(user,supersetUserInfo._1,roleIds)} )
      /*
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )
      */
      org <- stepOverF( Try{ckanApiClient.getOrganizationAsAdmin(groupCn)} )
      c <- step( a, Try{ckanApiClient.putUserInOrganizationAsAdmin(userName,org)} )

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


    val result = for {
      user <- stepOverF(Try{apiClientIPA.findUserByUid(userName)})

      a1 <- step(Try{apiClientIPA.removeUsersFromGroup(groupCn, Seq(userName))})

      supersetUserInfo <- stepOverF(Try{supersetApiClient.findUser(userName)})
      roleNames = supersetUserInfo._2.toList.filter(p => (!p.equals(toSupersetRole(groupCn)))); roleIds <- stepOverF(Try{supersetApiClient.findRoleIds(roleNames: _*)})

      a <- step(Try{supersetApiClient.updateUser(user, supersetUserInfo._1, roleIds)})
      /*
      a <- EitherT( supersetApiClient.deleteUser(supersetUserInfo._1) )
      b <- EitherT( supersetApiClient.createUserWithRoles(user,roleIds:_*) )*/

      org <- stepOverF(Try{ckanApiClient.getOrganizationAsAdmin(groupCn)})
      c <- step(Try{ckanApiClient.removeUserInOrganizationAsAdmin(userName, org)})

      //d <- EitherT( grafanaApiClient ) TODO re-enable when Grafana is integrated (review)
    } yield c

    result.value

  }

  def removeUserFromOrganization(groupCn:String, userName:String):Future[Either[Error,Success]] = {

    if( groupCn.equals(ConfigReader.defaultOrganization) )
      Future{ Left( Error(Option(1),Some("Cannot remove users from default organization"),None) ) }

    else{

      val result = for {
        user <-  stepOverF( Try{apiClientIPA.findUserByUid(userName)} )
        a <- stepOver( Try{registrationService.testIfIsNotPredefinedUser(user)} )// cannot remove predefined user

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
