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

import java.util.Properties

import it.gov.daf.common.config.Read

import scala.concurrent.duration._

case class FileExportConfig(numSessions: Int,
                            sizeThreshold: Int,
                            exportTimeout: FiniteDuration,
                            exportPath: String,
                            livyUrl: String,
                            livyProperties: Properties)

object FileExportConfig {

  private def readExport = Read.config("daf.export").!

  private def readLivyProperties(props: Properties = new Properties()) = for {
    connectionTimeout   <- Read.time    { "client.http.connection.timeout"        } default 10.seconds
    socketTimeout       <- Read.time    { "client.http.connection.socket.timeout" } default 5.minutes
    idleTimeout         <- Read.time    { "client.http.connection.idle.timeout"   } default 5.minutes
    compressionEnabled  <- Read.boolean { "client.http.content.compress.enable"   } default true
    initialPollInterval <- Read.time    { "client.http.job.initial_poll_interval" } default 100.milliseconds
    maxPollInterval     <- Read.time    { "client.http.job.max_poll_interval"     } default 5.seconds
  } yield {
    props.setProperty("livy.client.http.connection.timeout",        s"${connectionTimeout.toSeconds}s")
    props.setProperty("livy.client.http.connection.socket.timeout", s"${socketTimeout.toMinutes}m")
    props.setProperty("livy.client.http.connection.idle.timeout",   s"${idleTimeout.toMinutes}m")
    props.setProperty("livy.client.http.content.compress.enable",   compressionEnabled.toString)
    props.setProperty("livy.client.http.job.initial-poll-interval", s"${initialPollInterval.toMillis}ms")
    props.setProperty("livy.client.http.job.max-poll-interval",     s"${maxPollInterval.toSeconds}s")
    props
  }

  private def readValues = for {
    numSessions     <- Read.int    { "num_sessions"     } default 1
    sizeThreshold   <- Read.int    { "size_threshold"   } default 5120
    exportTimeout   <- Read.time   { "timeout"          } default 10.minutes
    exportPath      <- Read.string { "export_path"      }.!
    livyUrl         <- Read.string { "livy.url"         }.!
    livyProperties  <- readLivyProperties()
  } yield FileExportConfig(
    numSessions    = numSessions,
    sizeThreshold  = sizeThreshold,
    exportTimeout  = exportTimeout,
    exportPath     = exportPath,
    livyUrl        = livyUrl,
    livyProperties = livyProperties
  )

  def reader = readExport ~> readValues

}
