import com.google.inject.Singleton
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.{HdfsConfiguration, MiniDFSCluster}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication

import scala.util.{Failure, Try}

@Singleton
@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var", "org.wartremover.warts.TryPartial"))
class ServiceSpec extends Specification with BeforeAfterAll with WithFileSystem {

  def application: Application = GuiceApplicationBuilder().bindings(Seq(bind[WithFileSystem].to[ServiceSpec])).build()

  var miniCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)

  var fs: Try[FileSystem] = Failure[FileSystem](new Exception)

  "The 'Hello world' string" should {
    "contain 11 characters" in new WithApplication(application) {
      "Hello world" must have size (11)
    }
  }

  override def beforeAll(): Unit = {
    miniCluster = Try(new MiniDFSCluster.Builder(new HdfsConfiguration()).build())
    fs = miniCluster.map(_.getFileSystem)
  }

  override def afterAll(): Unit = {
    miniCluster.foreach(_.shutdown(true))
  }
}