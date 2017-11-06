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

import java.io.IOException
import java.net.ServerSocket

import net.opentsdb.core.TSDB
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseConfiguration, HBaseTestingUtility, TableName}
import org.apache.spark.SparkConf
import org.apache.spark.opentsdb.{OpenTSDBConfigurator, OpenTSDBContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Milliseconds, StreamingContext}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{WithServer, WsTestClient}
import shaded.org.hbase.async.HBaseClient
import net.opentsdb.utils.Config
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.MiniDFSCluster
import org.apache.hadoop.test.PathUtils
import play.api.libs.ws.{WSAuthScheme, WSResponse}
import better.files._
import scala.collection.convert.decorateAsScala._
import scala.collection.convert.decorateAsJava._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
class OpentsdbSpec extends Specification with BeforeAfterAll{

  import OpentsdbSpec._

  private val hbaseUtil = new HBaseTestingUtility()

  var openTSDBContext: OpenTSDBContext = _

  implicit var sparkSession: SparkSession = _

  var hbaseAsyncClient: HBaseClient = _

  var tsdb: TSDB = _

  var baseConf: Configuration = _

  override def beforeAll(): Unit = {
    OpenTSDBContext.saltWidth = 1
    OpenTSDBContext.saltBuckets = 2
    OpenTSDBContext.preloadUidCache = true
    hbaseUtil.startMiniCluster(4)

    val conf = new SparkConf().
      setAppName("spark-opentsdb-local-test").
      setMaster("local[4]").
      set("spark.io.compression.codec", "lzf")

    baseConf = hbaseUtil.getConfiguration

    val quorum = baseConf.get("hbase.zookeeper.quorum")
    val port = baseConf.get("hbase.zookeeper.property.clientPort")

    sparkSession = SparkSession.builder().config(conf).getOrCreate()

    HBaseConfiguration.merge(sparkSession.sparkContext.hadoopConfiguration, baseConf)

    //streamingContext = new StreamingContext(sparkSession.sparkContext, Milliseconds(200))
    openTSDBContext = new OpenTSDBContext(sparkSession, TestOpenTSDBConfigurator(baseConf))
    hbaseUtil.createTable(TableName.valueOf("tsdb-uid"), Array("id", "name"))
    hbaseUtil.createTable(TableName.valueOf("tsdb"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-tree"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-meta"), Array("name"))
    hbaseAsyncClient = new HBaseClient(s"$quorum:$port", "/hbase")
    val config = new Config(false)
    config.overrideConfig("tsd.storage.hbase.data_table", "tsdb")
    config.overrideConfig("tsd.storage.hbase.uid_table", "tsdb-uid")
    config.overrideConfig("tsd.core.auto_create_metrics", "true")
//    if (openTSDBContext.saltWidth > 0) {
//      config.overrideConfig("tsd.storage.salt.width", openTSDBContext.saltWidth.toString)
//      config.overrideConfig("tsd.storage.salt.buckets", openTSDBContext.saltBuckets.toString)
//    }
    config.overrideConfig("batchSize", "10")
    config.disableCompactions()
    tsdb = new TSDB(hbaseAsyncClient, config)

    Range(1,100).foreach(n => tsdb.addPoint("speed",System.currentTimeMillis(), n.toDouble, Map("tag" -> "value").asJava))


  }

  override def afterAll(): Unit = {
    //streamingContext.stop(false)
    //streamingContext.awaitTermination()
    sparkSession.stop()
    tsdb.shutdown()
    hbaseUtil.deleteTable("tsdb-uid")
    hbaseUtil.deleteTable("tsdb")
    hbaseUtil.deleteTable("tsdb-tree")
    hbaseUtil.deleteTable("tsdb-meta")
    hbaseUtil.shutdownMiniCluster()
  }


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

  def application: Application = GuiceApplicationBuilder().
    configure("hadoop_conf_dir" -> s"${OpentsdbSpec.confPath.pathAsString}").
    configure("pac4j.authenticator" -> "test").
    build()

  "The storage_manager" should {
    "read data from opentsdb" in new WithServer(app = application, port = getAvailablePort) {

        WsTestClient.withClient { implicit client =>
          val uri = "dataset:opentsdb:/opendata/test.parquet"
          val response: WSResponse = Await.result[WSResponse](client.
            url(s"http://localhost:$port/storage-manager/v1/physical-datasets?uri=$uri&format=opentsdb&limit=10").
            withAuth("david", "david", WSAuthScheme.BASIC).
            execute, Duration.Inf)
          response.body must beEqualTo("")
        }


    }
  }
}

class TestOpenTSDBConfigurator(mapConf: Map[String, String]) extends OpenTSDBConfigurator with Serializable {

  lazy val configuration: Configuration = mapConf.foldLeft(new Configuration(false)) { (conf, pair) =>
    conf.set(pair._1, pair._2)
    conf
  }

}

object TestOpenTSDBConfigurator {

  def apply(conf: Configuration): TestOpenTSDBConfigurator = new TestOpenTSDBConfigurator(
    conf.iterator().asScala.toList.map { entry => entry.getKey -> entry.getValue }.toMap[String, String]
  )

}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
object OpentsdbSpec {

  private val (testDataPath, confPath) = {
    val testDataPath = s"${PathUtils.getTestDir(classOf[ServiceSpec]).getCanonicalPath}/MiniCluster"
    val confPath = s"$testDataPath/conf"
    (
      testDataPath.toFile.createIfNotExists(asDirectory = true, createParents = false),
      confPath.toFile.createIfNotExists(asDirectory = true, createParents = false)
    )
  }

  var miniCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)

  var fileSystem: Try[FileSystem] = Failure[FileSystem](new Exception)

  lazy val sparkSession: SparkSession = SparkSession.builder().master("local").getOrCreate()

}

