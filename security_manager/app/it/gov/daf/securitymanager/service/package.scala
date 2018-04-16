package it.gov.daf.securitymanager

import it.gov.daf.common.sso.common.{CacheWrapper, LoginInfo}
import it.gov.daf.securitymanager.service.utilities.RequestContext
import it.gov.daf.sso.LoginClientLocal

package object service {

  def readLoginInfo()(implicit cacheWrapper:CacheWrapper)={

    val userName = RequestContext.getUsername()
    val pwd = cacheWrapper.getPwd(userName) match {
      case Some(x) =>x
      case None => throw new Exception("User passoword not in cache")
    }

    new LoginInfo( RequestContext.getUsername(), pwd, LoginClientLocal.HADOOP )
  }
}
