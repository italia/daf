package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.securitymanager.service.utilities.{AppConstants, BearerTokenGenerator, ConfigReader}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import security_manager.yaml.{Error, IpaUser, Success}
import it.gov.daf.common.authentication.Role
import cats.implicits._
import it.gov.daf.sso.ApiClientIPA
import play.api.Logger
import scala.concurrent.Future
import ProcessHandler._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.Try

@Singleton
class RegistrationService @Inject()(apiClientIPA:ApiClientIPA, supersetApiClient: SupersetApiClient, ckanApiClient: CkanApiClient, grafanaApiClient: GrafanaApiClient) {

  import security_manager.yaml.BodyReads._

  private val tokenGenerator = new BearerTokenGenerator

  /*
  private def wrapFuture1[T](in:Either[String,T]):Future[Either[Error,T]]={
    Future.successful {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(Error(Option(1), Some(l), None))
      }
    }

  }

  private def wrapFuture0[T](in:Either[String,T]):Future[Either[Error,T]]={
    Future.successful {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(Error(Option(0), Some(l), None))
      }
    }

  }

  private def evalInFuture1[T](in:Either[String,T]):Future[Either[Error,T]]={
    Future {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(Error(Option(1), Some(l), None))
      }
    }

  }

  private def evalInFuture0[T](in:Either[String,T]):Future[Either[Error,T]]={
    Future {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(Error(Option(0), Some(l), None))
      }
    }

  }
*/

  def requestRegistration(userIn:IpaUser):Future[Either[Error,MailService]] = {

    Logger.logger.info("requestRegistration")

    def cui = checkUserInfo(userIn)
    val result = for {
      a <- EitherT( wrapFuture1(checkUserInfo(userIn)) )
      user = formatRegisteredUser(userIn)
      b <- EitherT( checkRegistration(user.uid) )
      c <- EitherT( checkUser(user) )
      d <- EitherT( checkMail(user) )
      f <- EitherT( wrapFuture0(writeRequestNsendMail(user)(MongoService.writeUserData)) )
    } yield f

    result.value

  }


  def requestResetPwd(mail:String):Future[Either[Error,MailService]] = {

    Logger.logger.info("requestResetPwd")

    val result = for {
      user <- EitherT( apiClientIPA.findUserByMail(mail) )
      b <- EitherT( checkResetPwd(mail) )
      c <- EitherT( wrapFuture0(writeRequestNsendMail(user)(MongoService.writeResetPwdData)) )
    } yield c

    result.value

  }

  private def checkResetPwd( mail:String ) = {

    val result = MongoService.findResetPwdByMail(mail) match {
      case Right(o) => Left("Reset password already requested")
      case Left(o) => Right("Ok: not found")
    }

    wrapFuture1(result)
  }


  private def checkUserInfo(user:IpaUser):Either[String,String] ={

    if (user.userpassword.isEmpty || user.userpassword.get.length < 8)
      Left("Password minimum length is 8 characters")
    else if( !user.userpassword.get.matches("""^[a-zA-Z0-9%@#   &,;:_'/\<\(\[\{\\\^\=\$\!\|\]\}\)\u200C\u200B\?\*\-\+\.\>]*$""") )
      Left("Invalid chars in password")
    else if( !user.userpassword.get.matches("""^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$""") )
      Left("Password must contain al least one digit and one capital letter")
    else if( user.uid != null && !user.uid.isEmpty && !user.uid.matches("""^[a-z0-9_\-]*$""") )
      Left("Invalid chars in username")
    else if( !user.mail.matches("""^[a-z0-9_@\-\.]*$""") )
      Left("Invalid chars in mail")
    else
      Right("ok")

  }


  private def formatRegisteredUser(user: IpaUser): IpaUser = {

    if (user.uid == null || user.uid.isEmpty )
      user.copy( uid = user.mail.replaceAll("[@]", "_").replaceAll("[.]", "-"), role = Option(Role.Viewer.toString()) )
    else
      user

  }


  private def checkRegistration( uid:String ) = {

    val result = MongoService.findUserByUid(uid) match {
      case Right(o) => Left("Username already requested")
      case Left(o) => Right("Ok: not found")
    }

    wrapFuture1(result)
  }

  private def checkUser(user:IpaUser):Future[Either[Error,String]] = {

    apiClientIPA.findUserByUid(user.uid) map {
        case Right(r) => Left( Error(Option(1),Some("Username already registered"),None) )
        case Left(l) => Right("ok")
      }

  }


  private def checkMail(user:IpaUser):Future[Either[Error,String]] = {

    apiClientIPA.findUserByMail(user.mail)  map {
      case Right(r) => Left( Error(Option(1),Some("Mail already registered"),None) )
      case Left(l) => Right("ok")
    }

  }


  private def writeRequestNsendMail(user:IpaUser)(writeData:(IpaUser,String)=>Either[String,String]) : Either[String,MailService] = {

    Logger.logger.info("writeRequestNsendMail")
    val token = tokenGenerator.generateMD5Token(user.uid)

    writeData(user,token) match{
      case Right(r) => Right(new MailService(user.mail,token))
      case Left(l) => Left("Error writing in mongodb ")
    }

  }

  def checkUserNcreate(userIn:IpaUser):Future[Either[Error,Success]] = {

    checkUserInfo(userIn) match{
      case Left(l) => Future {Left( Error(Option(1),Some(l),None) )}
      case Right(r) => checkMailUidNcreateUser(userIn)
    }

  }

  def createUser(token:String): Future[Either[Error,Success]] = {

    MongoService.findAndRemoveUserByToken(token) match{
      case Right(json) => checkNcreateUser(json)
      case Left(l) => Future{ Left( Error(Option(1),Some("User pre-registration not found"),None) )}
    }

  }

  def resetPassword(token:String,newPassword:String): Future[Either[Error,Success]] = {

    val result = for {
      uid <- EitherT( readUidFromResetPwdRequest(token) )
      resetResult <- EitherT( apiClientIPA.resetPwd(uid))
      c <- EitherT( apiClientIPA.changePassword(uid,resetResult.fields.get,newPassword) )
    } yield c

    result.value

  }

  private def readUidFromResetPwdRequest(token:String) : Future[Either[Error,String]] = {
    MongoService.findAndRemoveResetPwdByToken(token) match{
      case Right(json) => (json \ "uid").asOpt[String] match {
        case Some(u) => Future{Right(u)}
        case None => Future{ Left( Error(Option(0),Some("Reset password: error in data reading "),None) )}
      }
      case Left(l) => Future{ Left( Error(Option(1),Some("Reset password request not found"),None) )}
    }
  }

  private def checkNcreateUser(json:JsValue):Future[Either[Error,Success]] = {

    Logger.logger.debug("checkNcreateUser input json: "+json)

    val result = json.validate[IpaUser]
    result match {
      case s: JsSuccess[IpaUser] =>  checkMailUidNcreateUser(s.get)
      case e: JsError => Logger.logger.error("data conversion errors"+e.errors); Future{ Left( Error(Option(0),Some("Error during user data conversion"),None) )}
    }

  }

  private def checkMailUidNcreateUser(user:IpaUser):Future[Either[Error,Success]] = {

    apiClientIPA.findUserByUid(user.uid) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Username already registered"), None))}
        case Left(l) => checkMailNcreateUser(user,false)
      }

    }
  }


  def checkMailNcreateUser(user:IpaUser,isPredefinedOrgUser:Boolean):Future[Either[Error,Success]] = {

    apiClientIPA.findUserByMail(user.mail) flatMap { result =>

      result match{
        case Right(r) => Future {Left(Error(Option(1), Some("Mail address already registered"), None))}
        case Left(l) =>  createUser(user,isPredefinedOrgUser)
      }

    }
  }


  private def createUser(user:IpaUser, isPredefinedOrgUser:Boolean):Future[Either[Error,Success]] = {

    Logger.logger.info("createUser")

    val result = for {
      a <- step( Try{apiClientIPA.createUser(user, isPredefinedOrgUser)} )
      a1 <- stepOver( Try{apiClientIPA.changePassword(user.uid,a.success.fields.get,user.userpassword.get)} )
      b <- stepOver( Try{apiClientIPA.addUsersToGroup(user.role.getOrElse(Role.Viewer.toString()),Seq(user.uid))} )
      c <- step( a, Try{addNewUserToDefaultOrganization(user)} )
    } yield c

    result.value.map{
      case Right(r) => Right( Success(Some("User created"), Some("ok")) )
      case Left(l) => if( l.steps !=0 ) {
        hardDeleteUser(user.uid).onSuccess { case e =>

          val steps = e.fold(ll=>ll.steps,rr=>rr.steps)
          if( l.steps != steps)
            throw new Exception( s"CreateUser rollback issue: process steps=${l.steps} rollback steps=$steps" )

        }

      }
        Left(l.error)
    }

  }

  private def hardDeleteUser(uid:String):Future[Either[ErrorWrapper,SuccessWrapper]] = {

    val result = for {

      b <- step( Try{apiClientIPA.deleteUser(uid)} )
      userInfo <- stepOverF( Try{supersetApiClient.findUser(uid)} )
      c <- step( Try{supersetApiClient.deleteUser(userInfo._1)} )
      // Commented because ckan have problems to recreate again the same user TODO try to test a ckan config not create local users
      //defOrg <- EitherT( ckanApiClient.getOrganizationAsAdmin(ConfigReader.defaultOrganization) )
      //d <- EitherT( ckanApiClient.removeUserInOrganizationAsAdmin(uid,defOrg) )
    } yield c

    result.value

  }

  def deleteUser(uid:String):Future[Either[Error,Success]] = {

    Logger.logger.info("deleteUser")

    val result = for {
      user <- stepOverF( Try{apiClientIPA.findUserByUid(uid)} )
      a1 <- stepOver( Try{testIfIsNotPredefinedUser(user)} )// cannot cancel predefined user
      a2 <- stepOver( Try{testIfUserBelongsToGroup(user)} )// cannot cancel user belonging to some orgs

      b <- EitherT( hardDeleteUser(uid) )

      // Commented because ckan have problems to recreate again the same user TODO try to test a ckan config not create local users
      //defOrg <- EitherT( ckanApiClient.getOrganizationAsAdmin(ConfigReader.defaultOrganization) )
      //d <- EitherT( ckanApiClient.removeUserInOrganizationAsAdmin(uid,defOrg) )
    } yield b

    result.value.map{
      case Right(r) => Right( Success(Some("User deleted"), Some("ok")) )
      case Left(l) => if( l.steps == 0 )
        Left(l.error)
      else
        throw new Exception( s"DeleteUser process issue: process steps=${l.steps}" )

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