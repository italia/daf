import java.io.{IOException, File => JFile}
import java.net.ServerSocket

import better.files._
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.{HdfsConfiguration, MiniDFSCluster}
import org.apache.hadoop.test.PathUtils
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Try}

final case class Person(name: String, age: Int)

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Throw"))
class ServiceSpec extends Specification with BeforeAfterAll {

  import ServiceSpec._

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

  def application: Application = GuiceApplicationBuilder().build()

  "test server logic" in new WithServer(app = application, port = getAvailablePort) {

    val sparkSession = SparkSession.builder().master("local").getOrCreate()

    import sparkSession.implicits._

    val persons = (1 to 1000).map(i => Person(s"Andy$i", Random.nextInt(85))).toSeq

    val caseClassDS = persons.toDS()//.repartition(10)

    caseClassDS.write.format("com.databricks.spark.avro").mode(SaveMode.Overwrite).save("/opendata/test.avro")

    WsTestClient.withClient { implicit client =>
      val uri = "dataset:hdfs:/opendata/test.avro"
      val response: WSResponse = Await.result[WSResponse](client.url(s"http://localhost:$port/storage_manager/v1/dataset?uri=$uri&format=avro").execute, Duration.Inf)
      println(response)
    }

    sparkSession.stop()
  }

  override def beforeAll(): Unit = {
    val conf = new HdfsConfiguration()
    conf.setBoolean("dfs.permissions", true)
    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA)
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, testDataPath.pathAsString)
    val builder = new MiniDFSCluster.Builder(conf)
    miniCluster = Try(builder.build())
    fileSystem = miniCluster.map(_.getFileSystem)
    fileSystem.foreach(fs => {
      val confFile: File = confPath / "hdfs-site.xml"
      for {os <- confFile.newOutputStream.autoClosed} fs.getConf.writeXml(os)
    })
    HadoopConfDir.hadoopConfDir = Some(ServiceSpec.confPath.pathAsString)
  }

  override def afterAll(): Unit = {
    miniCluster.foreach(_.shutdown(true))
    val _ = testDataPath.parent.parent.delete(true)
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
object ServiceSpec {

  val (testDataPath, confPath): (File, File) = {
    val testDataPath = s"${PathUtils.getTestDir(classOf[ServiceSpec]).getCanonicalPath}/MiniCluster"
    val confPath = s"$testDataPath/conf"
    (
      testDataPath.toFile.createIfNotExists(asDirectory = true, createParents = false),
      confPath.toFile.createIfNotExists(asDirectory = true, createParents = false)
    )
  }

  var miniCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)

  var fileSystem: Try[FileSystem] = Failure[FileSystem](new Exception)
}