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

case class ImpalaConfig(host: String, port: Int, kerberosPrincipal: String, poolConfig: JdbcPoolConfig) {

  val jdbcUrl = s"jdbc:hive2://$host:$port/;principal=$kerberosPrincipal"

}

object ImpalaConfig {

  private def readConfig = Read.config { "impala" }.!

  private def readJdbcPool = Read.config { "daf.jdbc.impala" }.! ~> JdbcPoolConfig.read

  private def readValues(jdbcPoolConfig: JdbcPoolConfig) = for {
    host      <- Read.string { "host" }.!
    port      <- Read.int    { "port" }.!
    principal <- Read.string { "principal" }.!
  } yield ImpalaConfig(
    host              = host,
    port              = port,
    kerberosPrincipal = principal,
    poolConfig        = jdbcPoolConfig)

  def reader = for {
    jdbcPoolConfig <- readJdbcPool
    impalaConfig   <- readConfig ~> readValues(jdbcPoolConfig)
  } yield impalaConfig

}
