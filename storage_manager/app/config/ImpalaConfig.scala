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
  * Container for configuration settings for the impala JDBC client.
  * @note This container exposes two flavors of JDBC urls: one connecting impala clients and another connecting hive2.
  * @param host the host name for an impala daemon
  * @param port the port where the hive2 server is running on the impala daemon
  * @param kerberosConfig the kerberos configuration
  * @param sslConfig optional SSL configuration to be used by the JDBC client
  */
case class ImpalaConfig(host: String, port: Int, kerberosConfig: ImpalaKerberosConfig, sslConfig: Option[ImpalaSSLConfig]) {

  private def impalaKerberosPart = s";KrbRealm=${kerberosConfig.realm};KrbHostFQDN=${kerberosConfig.domain};KrbServiceName=${kerberosConfig.service}"

  /**
    * The JDBC url for connecting impala JDBC clients.
    */
  val impalaUrl = s"jdbc:impala://$host:$port/;AuthMech=1$impalaKerberosPart"


  private def hiveKerberosPart = s";principal=${kerberosConfig.principal}"

  /**
    * The JDBC url for conntecting hive2 JDBC clients
    */
  val hiveUrl = s"jdbc:hive2://$host:$port/$hiveKerberosPart"

}

object ImpalaConfig {

  private def readConfig = Read.config { "impala" }.!

  private def readValues = for {
    host           <- Read.string { "host" }.!
    port           <- Read.int    { "port" }.!
    kerberosConfig <- ImpalaKerberosConfig.reader
    sslConfig      <- ImpalaSSLConfig.reader
  } yield ImpalaConfig(
    host      = host,
    port      = port,
    kerberosConfig = kerberosConfig,
    sslConfig = sslConfig
  )

  def reader = readConfig ~> readValues

}
