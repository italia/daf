package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.securitymanager.service.utilities.{AppConstants, BearerTokenGenerator, ConfigReader}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import security_manager.yaml.{Error, IpaUser, Success, UserList}
import it.gov.daf.common.authentication.Role
import cats.implicits._
import it.gov.daf.common.utils.Credentials
import it.gov.daf.sso.ApiClientIPA

import scala.concurrent.Future

@Singleton
class RegistrationService @Inject()(apiClientIPA:ApiClientIPA, supersetApiClient: SupersetApiClient, ckanApiClient: CkanApiClient, grafanaApiClient: GrafanaApiClient) {

  import security_manager.yaml.BodyReads._
  import play.api.libs.concurrent.Execution.Implicits._
  private val tokenGenerator = new BearerTokenGenerator


  def requestRegistration(userIn:IpaUser):Future[Either[String,MailService]] = {

    println("requestRegistration")

    checkUserInfo(userIn) match{

      case Left(l) => Future {Left(l)}
      case Right(r) => {

        val user = formatRegisteredUser(userIn)
        MongoService.findUserByUid(user.uid) match {
          case Right(o) => Future{Left("Username already requested")}
          case Left(o) => checkUserNregister(user)
        }

      }

    }

  }

  def checkUserNcreate(userIn:IpaUser):Future[Either[Error,Success]] = {

    checkUserInfo(userIn) match{
      case Left(l) => Future {Left( Error(Option(1),Some(l),None) )}
      case Right(r) => checkMailUidNcreateUser(userIn)
    }

  }

  private def checkUserInfo(user:IpaUser):Either[String,String] ={

    if (user.userpassword.isEmpty || user.userpassword.get.length < 8)
      Left("Password minimum length is 8 characters")
    else if( !user.userpassword.get.matches("^[a-zA-Z0-9%@#   &,;:_'/\\\\<\\\\(\\\\[\\\\{\\\\\\\\\\\\^\\\\-\\\\=\\\\$\\\\!\\\\|\\\\]\\\\}\\\\)\u200C\u200B\\\\?\\\\*\\\\+\\\\.\\\\>]*$") )
      Left("Invalid chars in password")
    else if( !user.userpassword.get.matches("""^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$""") )
      Left("Password must contain al least one digit and one capital letter")
    else if( user.uid != null && !user.uid.isEmpty && !user.uid.matches("^[a-z0-9_\\\\-]*$") )
      Left("Invalid chars in username")
    else if( !user.mail.matches("^[a-z0-9_@\\\\-\\\\.]*$") )
      Left("Invalid chars in mail")
    else
      Right("ok")

  }


  private def formatRegisteredUser(user: IpaUser): IpaUser = {

    println("uid-->"+user.uid)
    if (user.uid == null || user.uid.isEmpty )
      user.copy( uid = user.mail.replaceAll("[@]", "_").replaceAll("[.]", "-"), role = Option(Role.Viewer.toString()) )
    else
      user

  }


  private def checkUserNregister(user:IpaUser):Future[Either[String,MailService]] = {

    apiClientIPA.findUserByUid(user.uid) flatMap { result =>

      result match{
        case Right(r) => Future { Left("Username already registered") }
        case Left(l) => checkMailNregister(user)
      }

    }

  }


  private def checkMailNregister(user:IpaUser):Future[Either[String,MailService]] = {

    apiClientIPA.findUserByMail(user.mail) flatMap { result =>

      result match{
        case Right(r) => Future { Left("Mail already registered") }
        case Left(l) => Future { Right(registration(user)) }
      }

    }

  }


  private def registration(user:IpaUser):MailService = {

    val token = tokenGenerator.generateMD5Token(user.uid)

    MongoService.writeUserData(user,token)

    new MailService(user.mail,token)

  }


  def createUser(token:String): Future[Either[Error,Success]] = {

    MongoService.findAndRemoveUserByToken(token) match{
      case Right(json) => checkNcreateUser(json)
      case Left(l) => Future{ Left( Error(Option(1),Some("User pre-registration not found"),None) )}
    }

  }

  private def checkNcreateUser(json:JsValue):Future[Either[Error,Success]] = {

    val result = json.validate[IpaUser]
    result match {
      case s: JsSuccess[IpaUser] =>  checkMailUidNcreateUser(s.get)
      case e: JsError => Future{ Left( Error(Option(0),Some("Error during user data conversion"),None) )}
    }

  }

  private def checkMailUidNcreateUser(user:IpaUser):Future[Either[Error,Success]] = {

    apiClientIPA.findUserByUid(user.uid) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Username already registered"), None))}
        case Left(l) => checkMailNcreateUser(user)
      }

    }
  }


  private def checkMailNcreateUser(user:IpaUser):Future[Either[Error,Success]] = {

    apiClientIPA.findUserByMail(user.mail) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Mail address already registered"), None))}
        case Left(l) =>  createUser(user,false)
      }

    }
  }


  def createUser(user:IpaUser, isPredefinedOrgUser:Boolean):Future[Either[Error,Success]] = {

    //val userId = UserList(Option(Seq(user.uid)))

    val result = for {
      a <- EitherT( apiClientIPA.createUser(user, isPredefinedOrgUser) )
      a1 <- EitherT( apiClientIPA.changePassword(user.uid,a.fields.get,user.userpassword.get) )
      b <- EitherT( apiClientIPA.addUsersToGroup(user.role.getOrElse(Role.Viewer.toString()),Seq(user.uid)) )
      c <- EitherT( addNewUserToDefaultOrganization(user) )
    } yield c

    result.value.map{
      case Right(r) => Right( Success(Some("User created"), Some("ok")) )
      case Left(l) => Left(l)
    }

  }

  def createDefaultUser(user:IpaUser):Future[Either[Error,Success]] = {

    val result = for {
      a <- EitherT( apiClientIPA.createUser(user,true) )
      b <- EitherT( apiClientIPA.addUsersToGroup(user.role.getOrElse(Role.Viewer.toString()),Seq(user.uid)) )
      c <- EitherT( addDefaultUserToDefaultOrganization(user) )
    } yield c
    result.value

  }


  def testIfIsNotPredefinedUser(user:IpaUser):Future[Either[Error,Success]] = {

    if( user.title.isEmpty || (!user.title.get.equals(AppConstants.PredefinedOrgUserTitle)) )
      Future{Right( Success(Some("Ok"), Some("ok")))}
    else
      Future{Left(Error(Option(1), Some("Predefined user"), None))}

  }

  private def testIfUserBelongsToGroup(user:IpaUser):Future[Either[Error,Success]] = {

    if( user.organizations.isEmpty || user.organizations.get.isEmpty ||
        user.organizations.get.filter( p => (!p.equals(ConfigReader.defaultOrganization)) ).isEmpty
    )
      Future{Right( Success(Some("Ok"), Some("ok")))}
    else
      Future{Left(Error(Option(1), Some("User belongs to organization"), None))}

  }

  def testIfUserBelongsToThisGroup(user:IpaUser,groupCn:String):Future[Either[Error,Success]] = {

    if( user.organizations.isEmpty || user.organizations.get.isEmpty ||
      (!user.organizations.get.contains(groupCn))
    )
      Future{Right( Success(Some("Ok"), Some("ok")))}
    else
      Future{Left(Error(Option(1), Some("User belongs to this organization"), None))}

  }

  private def checkRole(role:String):Future[Either[Error,Success]] = {

    if( ApiClientIPA.isValidRole(role) )
      Future{Right( Success(Some("Ok"), Some("ok")))}
    else
      Future{Left(Error(Option(1), Some("Invalid role"), None))}

  }


  def deleteUser(uid:String):Future[Either[Error,Success]] = {

    val result = for {
      user <- EitherT( apiClientIPA.findUserByUid(uid) )
      a1 <- EitherT( testIfIsNotPredefinedUser(user) )// cannot cancel predefined user
      a2 <- EitherT( testIfUserBelongsToGroup(user) )// cannot cancel user belonging to some orgs
      b <- EitherT( apiClientIPA.deleteUser(uid) )
      userInfo <- EitherT( supersetApiClient.findUser(uid) )
      c <- EitherT( supersetApiClient.deleteUser(userInfo._1) )
      // Commented because ckan have problems to recreate again the same user TODO try to test a ckan config not create local users
      //defOrg <- EitherT( ckanApiClient.getOrganizationAsAdmin(ConfigReader.defaultOrganization) )
      //d <- EitherT( ckanApiClient.removeUserInOrganizationAsAdmin(uid,defOrg) )
    } yield c

    result.value

  }

  def updateUser(uid: String, givenname:String, sn:String, role:String ):Future[Either[Error,Success]]= {

    val result = for {
      a1 <- EitherT( checkRole(role) )
      user <- EitherT( apiClientIPA.findUserByUid(uid) )
      a <- EitherT( testIfIsNotPredefinedUser(user) )// cannot update predefined user

      b <- EitherT( apiClientIPA.removeUsersFromGroup(user.role.get,Seq(uid)) )
      b1 <- EitherT( apiClientIPA.addUsersToGroup(role,Seq(uid)) )
      c<- EitherT( apiClientIPA.updateUser(uid,givenname,sn) )

    } yield c

    result.value.map{
      case Right(r) => Right( Success(Some("User modified"), Some("ok")) )
      case Left(l) => Left(l)
    }
  }


  private def addNewUserToDefaultOrganization(ipaUser:IpaUser):Future[Either[Error,Success]] = {

    require(ipaUser.userpassword.nonEmpty,"user password needed!")

    //val userId = UserList(Option(Seq(ipaUser.uid)))

    val result = for {
      a <- EitherT( apiClientIPA.addUsersToGroup(ConfigReader.defaultOrganization,Seq(ipaUser.uid)) )
      roleIds <- EitherT( supersetApiClient.findRoleIds(ConfigReader.suspersetOrgAdminRole,IntegrationService.toSupersetRole(ConfigReader.defaultOrganization)) )
      b <- EitherT( supersetApiClient.createUserWithRoles(ipaUser,roleIds:_*) )
      //c <- EitherT( grafanaApiClient.addNewUserInOrganization(ConfigReader.defaultOrganization,ipaUser.uid,ipaUser.userpassword.get) ) TODO da riattivare
    } yield b

    result.value
  }


  private def addDefaultUserToDefaultOrganization(ipaUser:IpaUser):Future[Either[Error,Success]] = {

    require(ipaUser.userpassword.nonEmpty,"user password needed!")

    //val userId = UserList(Option(Seq(ipaUser.uid)))

    val result = for {
      a <- EitherT( apiClientIPA.addUsersToGroup(ConfigReader.defaultOrganization,Seq(ipaUser.uid)) )
      roleIds <- EitherT( supersetApiClient.findRoleIds(ConfigReader.suspersetOrgAdminRole,IntegrationService.toSupersetRole(ConfigReader.defaultOrganization)) )
      b <- EitherT( supersetApiClient.createUserWithRoles(ipaUser,roleIds:_*) )
      //c <- EitherT( grafanaApiClient.addNewUserInOrganization(ConfigReader.defaultOrganization,ipaUser.uid,ipaUser.userpassword.get) )
    } yield b

    result.value
  }



}