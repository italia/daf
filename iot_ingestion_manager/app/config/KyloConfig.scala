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

case class KyloConfig(host: String,
                      port: Int,
                      username: String,
                      password: String,
                      nifiBasePath: String) {

  val kyloBaseUrl = s"http://$host:$port"

}

object KyloConfig {

  private val readConfig = Read.config { "kylo" }.!

  private val readValues = for {
    host         <- Read.string { "host" }.!
    port         <- Read.int    { "port" }.!
    username     <- Read.string { "username" }.!
    password     <- Read.string { "password" }.!
    nifiBasePath <- Read.string { "nifi_base_path" } default "/nifi-api"
  } yield KyloConfig(
    host         = host,
    port         = port,
    username     = username,
    password     = password,
    nifiBasePath = nifiBasePath
  )

}
