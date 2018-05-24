package daf.dataset.export

import java.io.File
import java.net.URI
import java.util.Properties
import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.util.Timeout
import it.teamdigitale.filesystem._
import it.teamdigitale.filesystem.export.FileExportJob
import it.teamdigitale.instances.{ AkkaInstance, ConfigurationInstance }
import org.apache.commons.lang3.concurrent.ConcurrentUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.livy.{ Job, JobHandle, LivyClient, LivyClientFactory }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{ Failure, Random, Success }

class FileExportActorSpec extends WordSpec with Matchers with AkkaInstance with ConfigurationInstance with BeforeAndAfterAll {

  private implicit val askTimeout = Timeout.durationToTimeout { 2.seconds }

  private implicit val fileSystem = FileSystem.getLocal(new Configuration)

  private implicit lazy val executionContext = system.dispatchers.lookup("test-dispatcher")

  private val baseDir = "test-dir".asHadoop

  private val workingDir = baseDir / f"file-export-actor-spec-${Random.nextInt(10000)}%05d"

  private lazy val actorRef = system.actorOf {
    FileExportActor.props(
      new FileExportLivyClientFactory,
    "",
      new Properties,
      workingDir.asUriString
    )
  }

  override def beforeAll() = startAkka()

  "A file export actor" must {

    "return the path of the exported file" in {
      Await.result(
        actorRef ? ExportFile(workingDir.asUriString, RawFileFormat, JsonFileFormat),
        2.seconds
      ) match {
        case Success(resultPath: String) => resultPath.asHadoop.getParent should be { workingDir }
        case Success(somethingElse)      => fail { s"Received a Success of [$somethingElse] while expecting a path" }
        case Failure(error)              => fail(error)
        case somethingOther              => fail { s"Received unexpected reply [$somethingOther]" }
      }
    }

  }

}

sealed class FileExportLivyClientFactory extends LivyClientFactory {

  def createClient(uri: URI, properties: Properties) = new FileExportLivyClient

}

sealed class FileExportLivyClient extends LivyClient {

  def submit[A](job: Job[A]) = job match {
    case exportJob: FileExportJob => new CompletedJobHandle(exportJob.to.path.asInstanceOf[A])
    case _                        => throw new UnsupportedOperationException(s"File export client cannot deal with jobs of type [${job.getClass.getSimpleName}]")
  }

  def run[A](job: Job[A]) = submit(job)

  def stop(b: Boolean) = ()

  def uploadJar(file: File) = ConcurrentUtils.constantFuture { () }

  def addJar(uri: URI) = ConcurrentUtils.constantFuture { () }

  def uploadFile(file: File) = ConcurrentUtils.constantFuture { () }

  def addFile(uri: URI) = ConcurrentUtils.constantFuture { () }

}

sealed class CompletedJobHandle[A](expected: A) extends JobHandle[A] {

  def getState = JobHandle.State.SUCCEEDED

  def addListener(listener: JobHandle.Listener[A]) = throw new UnsupportedOperationException("Cannot add listeners to a completed handle")

  def cancel(mayInterruptIfRunning: Boolean) = false

  def isCancelled = false

  def isDone = true

  def get() = expected

  def get(timeout: Long, unit: TimeUnit) = expected

}
