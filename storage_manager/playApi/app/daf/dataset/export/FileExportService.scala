package daf.dataset.export

import java.util.Properties

import akka.actor.ActorRefFactory
import akka.pattern.ask
import akka.routing.RoundRobinPool
import it.gov.daf.common.config._
import it.teamdigitale.filesystem._
import org.apache.hadoop.fs.{ FileSystem, Path }
import org.apache.livy.client.http.HttpClientFactory
import play.api.Configuration

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class FileExportService(configuration: Configuration)(implicit actorRefFactory: ActorRefFactory, fileSystem: FileSystem) {

  private val livyConfig = FileExportServiceConfig.reader.read(configuration) match {
    case Success(config) => config
    case Failure(error)  => throw ConfigReadException("Unable to read export configuration", error)
  }

  private val exportRouter = actorRefFactory.actorOf {
    RoundRobinPool(livyConfig.numSessions).props {
      FileExportActor.props(new HttpClientFactory, livyConfig)
    }
  }

  implicit val executionContext = actorRefFactory.dispatcher

  def export(path: Path, fromFormat: FileDataFormat, toFormat: FileDataFormat): Future[String] =
    { exportRouter ? ExportFile(path.asUriString, fromFormat, toFormat) }.flatMap {
      case Success(dataPath: String) => Future.successful { dataPath }
      case Success(invalidValue)     => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a string but got: [$invalidValue]") }
      case Failure(error)            => Future.failed { error }
      case unexpectedValue           => Future.failed { new IllegalArgumentException(s"Unexpected value received from export service; expected a Try[String], but received [$unexpectedValue]") }
    }

}

case class FileExportServiceConfig(numSessions: Int,
                                   exportPath: String,
                                   livyUrl: String,
                                   livyProperties: Properties)

private object FileExportServiceConfig {

  private def readExport = Read.config("export").!

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
    numSessions    <- Read.int    { "num_sessions" } default 1
    exportPath     <- Read.string { "export_path"  }.!
    livyUrl        <- Read.string { "livy.url"     }.!
    livyProperties <- readLivyProperties()
  } yield FileExportServiceConfig(
    numSessions    = numSessions,
    exportPath     = exportPath,
    livyUrl        = livyUrl,
    livyProperties = livyProperties
  )

  def reader = readExport ~> readValues

}