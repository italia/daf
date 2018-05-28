package it.teamdigitale.web

import it.gov.daf.common.web.{ Impersonation, WebAction }
import org.apache.hadoop.security.UserGroupInformation

import scala.language.higherKinds

sealed class HadoopImpersonation(proxyUser: UserGroupInformation) extends Impersonation {

  def wrap[F[_], A](action: WebAction[F, A]) = { userId => request =>
    (proxyUser as userId) { action(request) }
  }

}

object Impersonations {

  def hadoop(implicit proxyUser: UserGroupInformation) = new HadoopImpersonation(proxyUser)

}
