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

import scala.concurrent.duration._

/**
  * Container for configuration of export file cleanup functionality.
  * @param pollInterval the amount of time to wait before cleaning the export path again after a successful attempt
  * @param backOffInterval the amount of time to wait before reattempting a failed cleanup attempt
  * @param maxAge the longest an exported directory is allowed to exist before it is cleaned
  * @param maxRetries the number of times a failed attempt is retried
  */
case class FileExportCleanupConfig(pollInterval: FiniteDuration,
                                   backOffInterval: FiniteDuration,
                                   maxAge: FiniteDuration,
                                   maxRetries: Int)

object FileExportCleanupConfig {

  private def readCleanupConfig = Read.config { "cleanup" }.!

  private def readCleanupValues = for {
    pollInterval    <- Read.time { "poll_interval"     } default 5.minutes
    backOffInterval <- Read.time { "back_off_interval" } default 30.seconds
    maxAge          <- Read.time { "max_age"           } default 5.minutes
    maxRetries      <- Read.int  { "max_retries"       } default 4
  } yield FileExportCleanupConfig(
    pollInterval    = pollInterval,
    backOffInterval = backOffInterval,
    maxAge          = maxAge,
    maxRetries      = maxRetries
  )

  /**
    * Returns the `it.gov.daf.common.config.ConfigReader` that will read the file export cleanup configuration.
    */
  def read = readCleanupConfig ~> readCleanupValues

}
