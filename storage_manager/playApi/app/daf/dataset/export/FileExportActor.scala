package daf.dataset.export

import java.net.URI
import java.util.{ Properties, UUID }

import akka.actor.{ Actor, Props }
import it.teamdigitale.filesystem.export.FileExportJob
import it.teamdigitale.filesystem._
import org.apache.hadoop.fs.{ FileSystem, FileUtil }
import org.apache.livy.LivyClientFactory

import scala.util.{ Failure, Success, Try }

/**
  * Akka actor excapsulating functionality for file export operations. Upon creating a [[FileExportActor]], a Spark
  * session in Livy will be created in its [[preStart]] hook, and closed in its [[postStop]].
  *
  * The Actor will reply to its export requests by sending back the [[Try]] instance representing the [[Success]] or
  * [[Failure]] of the export. When it is successful, the path of the exported file will be passed along in the
  * [[Success]].
  *
  * @note A Spark job is only triggered in case the input and output formats are different, otherwise the input file is
  *       simply copied to an output location.
  *
  * @param livyFactory the client factory that will be used to create the livy client
  * @param livyUrl base url for the livy server
  * @param livyProps the [[Properties]] instance used to configure the livy client
  * @param exportPath string representing the base path where to put the exported data
  * @param fileSystem the [[FileSystem]] instance used for interaction
  */
class FileExportActor(livyFactory: LivyClientFactory,
                      livyUrl: String,
                      livyProps: Properties,
                      exportPath: String,
                      fileSystem: FileSystem) extends Actor {

  private val livyClient = livyFactory.createClient(URI.create(livyUrl), livyProps)

  private def suffix = UUID.randomUUID.toString.toLowerCase.split("-").take(3).mkString("-")

  private def outputPath(inputPath: String) = exportPath / s"${inputPath.asHadoop.getName}-$suffix"

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

  private def submit(inputPath: String, outputPath: String, fromFormat: FileDataFormat, toFormat: FileDataFormat) = Try {
    livyClient.run { FileExportJob.create(inputPath, outputPath, fromFormat, toFormat) }.get
  }

  override def preStart() = {
    livyClient.addJar { this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI }.get
  }

  override def postStop() = {
    livyClient.stop(true)
  }

  def receive = {
    case ExportFile(path, from, to) if from == to => sender ! copy(path, outputPath(path))
    case ExportFile(path, from, to)               => sender ! submit(path, outputPath(path), from, to)
  }


}

object FileExportActor {

  def props(livyFactory: LivyClientFactory,
            exportServiceConfig: FileExportServiceConfig)(implicit fileSystem: FileSystem): Props = props(
    livyFactory,
    exportServiceConfig.livyUrl,
    exportServiceConfig.livyProperties,
    exportServiceConfig.exportPath
  )

  def props(livyFactory: LivyClientFactory,
            livyUrl: String,
            livyProps: Properties,
            exportPath: String)(implicit fileSystem: FileSystem): Props = Props {
    new FileExportActor(
      livyFactory,
      livyUrl,
      livyProps,
      exportPath,
      fileSystem
    )
  }

}

sealed trait ExportMessage

case class ExportFile(path: String, sourceFormat: FileDataFormat, targetFormat: FileDataFormat) extends ExportMessage
