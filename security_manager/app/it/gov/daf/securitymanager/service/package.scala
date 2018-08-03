package it.gov.daf.securitymanager

import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo}
import it.gov.daf.securitymanager.service.utilities.RequestContext
import it.gov.daf.sso.LoginClientLocal
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future


package object service {

  //val ORG_REF_USER_POSTFIX: String = "_org_rif"
  val ORG_REF_USER_POSTFIX: String = "_default_admin" // for compatibility with old security model
  val WRK_REF_USER_POSTFIX: String = "_wrk_rif"

  def wrapFuture1[T](in: =>Either[String,T]):Future[Either[security_manager.yaml.Error,T]]={
    Future.successful {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(security_manager.yaml.Error(Option(1), Some(l), None))
      }
    }

  }

  def wrapFuture0[T](in: =>Either[String,T]):Future[Either[security_manager.yaml.Error,T]]={
    Future.successful {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(security_manager.yaml.Error(Option(0), Some(l), None))
      }
    }

  }

  def evalInFuture1[T](in: =>Either[String,T]):Future[Either[security_manager.yaml.Error,T]]={
    Future {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(security_manager.yaml.Error(Option(1), Some(l), None))
      }
    }

  }

  def evalInFuture0[T](in: =>Either[String,T]):Future[Either[security_manager.yaml.Error,T]]={
    Future {
      in match {
        case Right(r) => Right(r)
        case Left(l) => Left(security_manager.yaml.Error(Option(0), Some(l), None))
      }
    }

  }

  def evalInFuture1S(in: =>Either[String,String]):Future[Either[security_manager.yaml.Error,security_manager.yaml.Success]]={
    Future {
      in match {
        case Right(r) => Right( security_manager.yaml.Success(Some(r), Some("ok")) )
        case Left(l) => Left( security_manager.yaml.Error(Option(1), Some(l), None) )
      }
    }

  }

  def wrapInFuture1S(in: =>Either[String,String]):Future[Either[security_manager.yaml.Error,security_manager.yaml.Success]]={
    Future.successful {
      in match {
        case Right(r) => Right( security_manager.yaml.Success(Some(r), Some("ok")) )
        case Left(l) => Left( security_manager.yaml.Error(Option(1), Some(l), None) )
      }
    }

  }

  def evalInFuture0S(in: =>Either[String,String]):Future[Either[security_manager.yaml.Error,security_manager.yaml.Success]]={
    Future {
      in match {
        case Right(r) => Right( security_manager.yaml.Success(Some(r), Some("ok")) )
        case Left(l) => Left(security_manager.yaml.Error(Option(0), Some(l), None))
      }
    }

  }

  def readLoginInfo()(implicit cacheWrapper:CacheWrapper)={

    val userName = RequestContext.getUsername()
    val pwd = cacheWrapper.getPwd(userName) match {
      case Some(x) =>x
      case None => throw new Exception("User not in cache")
    }

    new LoginInfo( RequestContext.getUsername(), pwd, LoginClientLocal.HADOOP )
  }

  object Permission {

    sealed abstract class EnumVal(name : String){
      override def toString = name
    }

    case object read extends EnumVal("r-x")
    case object readWrite extends EnumVal("rwx")

    val permissions = Seq(read, readWrite)

  }


}
