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

package daf.dataset.export

import java.io.File
import java.net.URI
import java.util.{ Properties, UUID }

import akka.actor.{ Actor, Props, ReceiveTimeout }
import config.FileExportConfig
import daf.dataset.ExtraParams
import daf.filesystem._
import org.apache.commons.net.util.Base64
import org.apache.hadoop.fs.{ FileSystem, FileUtil }
import org.apache.livy.LivyClientFactory
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success, Try }

/**
  * Akka actor encapsulating functionality for file export operations. Upon creating a [[FileExportActor]], a Spark
  * session in Livy will be created in its [[preStart]] hook, and closed in its [[postStop]].
  *
  * The Actor will reply to its export requests by sending back the `Try` instance representing the `Success` or
  * `Failure` of the export. When it is successful, the path of the exported file will be passed along in the
  * `Success`.
  *
  * @note A Spark job is only triggered in case the input and output formats are different, otherwise the input file is
  *       simply copied to an output location.
  *
  * @param livyFactory the client factory that will be used to create the livy client
  * @param livyHost base url for the livy server
  * @param livyAuth optional Base64 encoded basic authorization data for livy connection; when this is set to `None`,
  *                 the connection is assumed to be http, other it will be set to https
  * @param livyAppJars a list of URLs containing the location of any JARs that should be added to the livy session; when
  *                    empty, the current code-source location is added instead
  * @param livyProps the `Properties` instance used to configure the livy client
  * @param kuduMaster the connection string for the Kudu cluster master
  * @param exportPath string representing the base path where to put the exported data
  * @param keepAliveTimeout the minimum amount of time to wait before triggering a keep-alive, which is an inexpensive
  *                         job that is triggered if livy is not utilized to keep the livy session alive
  * @param fileSystem the `FileSystem` instance used for interaction
  */
class FileExportActor(livyFactory: LivyClientFactory,
                      livyHost: String,
                      livyAuth: Option[String],
                      livySSL: Boolean,
                      livyAppJars: Seq[String],
                      livyProps: Properties,
                      kuduMaster: String,
                      exportPath: String,
                      keepAliveTimeout: FiniteDuration,
                      fileSystem: FileSystem) extends Actor {

  private val logger = LoggerFactory.getLogger("it.gov.daf.ExportActor")

  private val livyClientScheme = if (livySSL) "https" else "http"

  private val livyUrl = livyAuth.map { auth => new String(Base64.decodeBase64(auth)) } match {
    case Some(auth) => s"$livyClientScheme://$auth@$livyHost/"
    case None       => s"$livyClientScheme://$livyHost/"
  }

  private lazy val livyClient = livyFactory.createClient(URI.create(livyUrl), livyProps)

  private val livyAppJarURIs = livyAppJars.map { new File(_) } match {
    case seq if seq.isEmpty => Seq { new File(this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI) }
    case seq                => seq
  }

  private def suffix = UUID.randomUUID.toString.toLowerCase.split("-").take(3).mkString("-")

  private def outputPath(inputPath: String) = exportPath / s"${inputPath.asHadoop.getName}-$suffix"

  private def outputTable(name: String) = exportPath / s"$name-$suffix"

  private def outputQuery = exportPath / s"query-$suffix-$suffix"

  private def copy(inputPath: String, outputPath: String) = Try {
    FileUtil.copy(
      fileSystem,
      inputPath.asHadoop,
      fileSystem,
      outputPath.asHadoop,
      false,
      fileSystem.getConf
    )
  }.flatMap {
    case true  => Success(outputPath)
    case false => Failure { new RuntimeException("Failed to copy files; check that the destination directory is accessible or can be created") }
  }

  private def submitFileExport(inputPath: String, outputPath: String, fromFormat: FileDataFormat, toFormat: FileDataFormat, params: ExtraParams, limit: Option[Int]) = Try {
    livyClient.run { FileExportJob.create(inputPath, outputPath, fromFormat, toFormat, params, limit) }.get
  }

  private def submitKuduExport(tableName: String, outputPath: String, toFormat: FileDataFormat, params: ExtraParams, limit: Option[Int]) = Try {
    livyClient.run { KuduExportJob.create(tableName, kuduMaster, outputPath, toFormat, params, limit) }.get
  }

  private def submitQueryExport(query: String, outputPath: String, toFormat: FileDataFormat, params: ExtraParams) = Try {
    livyClient.run { QueryExportJob.create(query, outputPath, toFormat, params) }.get
  }

  private def keepAlive() = Try { livyClient.run { KeepAliveJob.init }.get } match {
    case Success(duration) => logger.info { s"Refreshed livy session within [$duration] milliseconds after [${keepAliveTimeout.toMinutes}] minutes of inactivity" }
    case Failure(error)    => logger.warn(s"Failed to refresh session after [${keepAliveTimeout.toMinutes}] minutes of inactivity", error)
  }

  override def preStart() = {
    livyAppJarURIs.foreach { livyClient.uploadJar(_).get }
    context.setReceiveTimeout { keepAliveTimeout }
  }

  override def postStop() = {
    livyClient.stop(true)
  }

  def receive = {
    case ExportFile(path, from, to, _, None) if from == to => sender ! copy(path, outputPath(path))
    case ExportFile(path, from, to, params, limit)         => sender ! submitFileExport(path, outputPath(path), from, to, params, limit)
    case ExportTable(name, to, params, limit)              => sender ! submitKuduExport(name, outputTable(name), to, params, limit)
    case ExportQuery(query, to, params)                    => sender ! submitQueryExport(query, outputQuery, to, params)
    case ReceiveTimeout                                    => keepAlive()
  }

}

object FileExportActor {

  def props(livyFactory: LivyClientFactory,
            kuduMaster: String,
            exportServiceConfig: FileExportConfig)(implicit fileSystem: FileSystem): Props = props(
    livyFactory,
    exportServiceConfig.livyHost,
    exportServiceConfig.livyAuth,
    exportServiceConfig.livySSL,
    exportServiceConfig.livyAppJars,
    exportServiceConfig.livyProperties,
    kuduMaster,
    exportServiceConfig.exportPath,
    exportServiceConfig.keepAliveTimeout
  )

  def props(livyFactory: LivyClientFactory,
            livyHost: String,
            livyAuth: Option[String],
            livySSL: Boolean,
            livyAppJars: Seq[String],
            livyProps: Properties,
            kuduMaster: String,
            exportPath: String,
            keepAliveTimeout: FiniteDuration)(implicit fileSystem: FileSystem): Props = Props {
    new FileExportActor(
      livyFactory,
      livyHost,
      livyAuth,
      livySSL,
      livyAppJars,
      livyProps,
      kuduMaster,
      exportPath,
      keepAliveTimeout,
      fileSystem
    )
  }

}

sealed trait ExportMessage

case class ExportFile(path: String,
                      sourceFormat: FileDataFormat,
                      targetFormat: FileDataFormat,
                      extraParams: Map[String, String] = Map.empty[String, String],
                      limit: Option[Int] = None) extends ExportMessage

case class ExportTable(name: String,
                       targetFormat: FileDataFormat,
                       extraParams: Map[String, String] = Map.empty[String, String],
                       limit: Option[Int] = None) extends ExportMessage

case class ExportQuery(query: String,
                       targetFormat: FileDataFormat,
                       extraParams: Map[String, String] = Map.empty[String, String]) extends ExportMessage