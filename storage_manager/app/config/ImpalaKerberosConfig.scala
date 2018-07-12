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

package config

import it.gov.daf.common.config.Read

/**
  * Container for Kerberos configuration for impala clients.
  * @param realm the string representing the realm to connect to, as configured in krb5
  * @param domain the fully-qualified domain name of the host that the principal should apply on
  * @param service the name of the principal, for example `impala` or `hive`
  */
case class ImpalaKerberosConfig(realm: String, domain: String, service: String) {

  /**
    * The string representing the full principal name.
    */
  val principal = s"$service/$domain@$realm"

}

object ImpalaKerberosConfig {

  private def readKerberosCOnfig = Read.config { "kerberos" }.!

  private def readKerberosValues = for {
    realm   <- Read.string { "realm"   }.!
    domain  <- Read.string { "domain"  }.!
    service <- Read.string { "service" }.!
  } yield ImpalaKerberosConfig(realm, domain, service)

  def reader = readKerberosCOnfig ~> readKerberosValues

}
