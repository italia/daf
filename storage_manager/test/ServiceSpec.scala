import java.io.{File, IOException}
import java.net.ServerSocket

import com.google.inject.Singleton
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.hdfs.{HdfsConfiguration, MiniDFSCluster}
import org.apache.hadoop.test.PathUtils
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

@Singleton
@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var", "org.wartremover.warts.Throw"))
class ServiceSpec extends Specification with BeforeAfterAll with WithFileSystem {

  def getAvailablePort: Int = {
    try {
      val socket = new ServerSocket(0)
      try {
        socket.getLocalPort
      } finally {
        socket.close()
      }
    } catch {
      case e: IOException =>
        throw new IllegalStateException(s"Cannot find available port: ${e.getMessage}", e)
    }
  }

  def application: Application = GuiceApplicationBuilder().bindings(Seq(bind[WithFileSystem].to[ServiceSpec])).build()

  var testDataPath: Try[File] = Failure[File](new Exception)

  var miniCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)

  var fs: Try[FileSystem] = Failure[FileSystem](new Exception)

  "test server logic" in new WithServer(app = application, port = getAvailablePort) {

    WsTestClient.withClient { implicit client =>
      val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage_manager/v1/dataset?uri=aaaaa").execute, Duration.Inf)
      println(response)
    }
  }

  override def beforeAll(): Unit = {
    val conf = new HdfsConfiguration()
    conf.setBoolean("dfs.permissions", true)
    testDataPath = Try(new File(PathUtils.getTestDir(getClass), "MiniCluster"))
    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA)
    testDataPath.foreach(path => conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, path.getAbsolutePath))
    val builder = new MiniDFSCluster.Builder(conf)
    miniCluster = Try(builder.build())
    fs = miniCluster.map(_.getFileSystem)
  }

  override def afterAll(): Unit = {
    miniCluster.foreach(_.shutdown(true))
    val localFileSystem = FileSystem.getLocal(new HdfsConfiguration())
    testDataPath.foreach(p => localFileSystem.delete(new Path(p.getParentFile.getParentFile.getAbsolutePath), true))
  }
}