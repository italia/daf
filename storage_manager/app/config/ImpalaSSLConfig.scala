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
  * Container for the SSL keystore location and password.
  * @param keystore the location of the keystore
  * @param password the password of the keystore, defaults to `changeit`
  */
case class ImpalaSSLConfig(keystore: String, password: String)

object ImpalaSSLConfig {

  private def readSSLConfig = Read.config { "ssl" }

  private def readSSLValues = for {
    keystore <- Read.string { "keystore" }.!
    password <- Read.string { "password" } default "changeit"
  } yield ImpalaSSLConfig(keystore, password)


  def reader = readSSLConfig ~?> readSSLValues
}
