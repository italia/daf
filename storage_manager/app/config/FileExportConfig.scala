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

/**
  * Container for configuration settings around the export functionality.
  * @param numSessions the maximum number of livy sessions to open and maintain
  * @param sizeThreshold the minimum dataset size in KB before a download is carried out by export
  * @param exportTimeout the maximum amount of time an export is allowed to take before it is killed
  * @param exportPath the base path where to store export results
  * @param livyHost the hostname (:port) for the livy server
  * @param livyAuth the auth information for the livy client
  * @param livyAppJars which additional jars to add to the livy client for upload to the server
  * @param livyProperties additional properties for configuration of the livy client
  * @param cleanup configuration for the export file cleanup functionality
  * @see [[FileExportCleanupConfig]]
  */
case class FileExportConfig(numSessions: Int,
                            sizeThreshold: Int,
                            exportTimeout: FiniteDuration,
                            exportPath: String,
                            livyHost: String,
                            livyAuth: Option[String],
                            livyAppJars: List[String],
                            livyProperties: Properties,
                            cleanup: FileExportCleanupConfig)

object FileExportConfig {

  private def readExportConfig = Read.config { "daf.export" }.!

  private def readLivyConfig   = Read.config { "livy" }.!

  private def readLivyProperties(props: Properties = new Properties()) = for {
    enableSpnego        <- Read.boolean { "client.http.spnego.enabled"            } default false
    jaasConf            <- Read.string  { "client.http.auth.login.conf"           }
    authScheme          <- Read.string  { "client.http.auth.scheme"               }
    krb5                <- Read.string  { "client.http.krb5.conf"                 }
    connectionTimeout   <- Read.time    { "client.http.connection.timeout"        } default 10.seconds
    socketTimeout       <- Read.time    { "client.http.connection.socket.timeout" } default 5.minutes
    idleTimeout         <- Read.time    { "client.http.connection.idle.timeout"   } default 5.minutes
    compressionEnabled  <- Read.boolean { "client.http.content.compress.enable"   } default true
    initialPollInterval <- Read.time    { "client.http.job.initial_poll_interval" } default 100.milliseconds
    maxPollInterval     <- Read.time    { "client.http.job.max_poll_interval"     } default 5.seconds
  } yield {
    props.setProperty("livy.client.http.spnego.enable",             enableSpnego.toString)
    props.setProperty("livy.client.http.connection.timeout",        s"${connectionTimeout.toSeconds}s")
    props.setProperty("livy.client.http.connection.socket.timeout", s"${socketTimeout.toMinutes}m")
    props.setProperty("livy.client.http.connection.idle.timeout",   s"${idleTimeout.toMinutes}m")
    props.setProperty("livy.client.http.content.compress.enable",   compressionEnabled.toString)
    props.setProperty("livy.client.http.job.initial-poll-interval", s"${initialPollInterval.toMillis}ms")
    props.setProperty("livy.client.http.job.max-poll-interval",     s"${maxPollInterval.toSeconds}s")

    jaasConf.foreach   { props.setProperty("livy.client.http.auth.login.config", _) }
    krb5.foreach       { props.setProperty("livy.client.http.krb5.conf",         _) }
    authScheme.foreach { props.setProperty("livy.client.http.auth.scheme",       _) }

    props
  }

  private def readExportValues = for {
    numSessions     <- Read.int     { "num_sessions"   } default 1
    sizeThreshold   <- Read.int     { "size_threshold" } default 5120
    exportTimeout   <- Read.time    { "timeout"        } default 10.minutes
    exportPath      <- Read.string  { "export_path"    }.!
    livyHost        <- Read.string  { "livy.host"      }.!
    livyAuth        <- Read.string  { "livy.auth"      }
    livyAppJars     <- Read.strings { "livy.jars"      } default List.empty[String]
    livyProperties  <- readLivyConfig ~> readLivyProperties()
    cleanup         <- FileExportCleanupConfig.read
  } yield FileExportConfig(
    numSessions    = numSessions,
    sizeThreshold  = sizeThreshold,
    exportTimeout  = exportTimeout,
    exportPath     = exportPath,
    livyHost       = livyHost,
    livyAuth       = livyAuth,
    livyAppJars    = livyAppJars,
    livyProperties = livyProperties,
    cleanup        = cleanup
  )

  /**
    * Returns the `ConfigReader` that will read the file export configuration.
    */
  def reader = readExportConfig ~> readExportValues

}