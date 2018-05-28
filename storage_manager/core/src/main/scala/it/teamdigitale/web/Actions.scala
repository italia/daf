package it.teamdigitale.web

import it.gov.daf.common.web.{ ActionBuilder, Actions => DefaultActions }
import org.apache.hadoop.security.UserGroupInformation

object Actions {

  def hadoop(implicit proxyUser: UserGroupInformation): ActionBuilder = DefaultActions.impersonated { Impersonations.hadoop }.checkedWith { ErrorHandlers.spark }

  def basic: ActionBuilder = DefaultActions.basic.checkedWith { ErrorHandlers.spark }

}
