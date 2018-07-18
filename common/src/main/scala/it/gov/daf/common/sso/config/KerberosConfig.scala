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

package it.gov.daf.common.sso.config

import it.gov.daf.common.config.{ ConfigReader, Read }

import scala.concurrent.duration._

final case class KerberosConfig(principal: String, keytab: String, refreshInterval: FiniteDuration)

object KerberosConfig {

  private val readConfig = Read.config { "kerberos" }.!

  private val readValues = for {
    principal       <- Read.string { "principal" }.!
    keytab          <- Read.string { "keytab"    }.!
    refreshInterval <- Read.time   { "refresh_interval" } default 1.hour
  } yield KerberosConfig(
    principal       = principal,
    keytab          = keytab,
    refreshInterval = refreshInterval
  )

  def reader: ConfigReader[KerberosConfig] = readConfig ~> readValues

}
