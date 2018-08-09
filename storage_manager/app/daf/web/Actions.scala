/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package daf.web

import it.gov.daf.common.web.{ ActionBuilder, Actions => DefaultActions }
import org.apache.hadoop.security.UserGroupInformation

object Actions {

  def hadoop(implicit proxyUser: UserGroupInformation): ActionBuilder = DefaultActions.impersonated { Impersonations.hadoop }.checkedWith { ErrorHandlers.api orElse ErrorHandlers.spark }

  def basic: ActionBuilder = DefaultActions.basic.checkedWith { ErrorHandlers.api orElse ErrorHandlers.security orElse ErrorHandlers.spark }

}
