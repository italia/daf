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

import ServiceSpec.confPath
import net.opentsdb.core.TSDB
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, HBaseTestingUtility, TableName}
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
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.omg.CORBA.TIMEOUT
import play.Logger
import shaded.org.hbase.async

import scala.collection.convert.decorateAsScala._
import scala.collection.convert.decorateAsJava._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.While"
  )
)class OpentsdbSpec extends Specification with BeforeAfterAll{

  import OpentsdbSpec._

  var hbaseUtil: HBaseTestingUtility = new HBaseTestingUtility()

  var hbaseAsyncClient: HBaseClient = _

  var tsdb: TSDB = _

  var baseConf: Configuration = _


  override def beforeAll(): Unit = {
    OpenTSDBContext.saltWidth = 1
    OpenTSDBContext.saltBuckets = 2
    OpenTSDBContext.preloadUidCache = true

    baseConf = hbaseUtil.getConfiguration


    println("Starting the HBase minicluster")
    //hbaseUtil.getConfiguration.set("test.hbase.zookeeper.property.clientPort", "2181")
    hbaseUtil.startMiniCluster(4)
    println("Started the HBase minicluster")

    val conf = new SparkConf().
      setAppName("spark-opentsdb-local-test").
      setMaster("local[4]").
      set("spark.io.compression.codec", "lzf")


    val confFile: File = confPath / "hbase-site.xml"
    for {os <- confFile.newOutputStream.autoClosed} baseConf.writeXml(os)

    val quorum = baseConf.get("hbase.zookeeper.quorum")
    val port = baseConf.get("hbase.zookeeper.property.clientPort")

    hbaseUtil.createTable(TableName.valueOf("tsdb-uid"), Array("id", "name"))
    hbaseUtil.createTable(TableName.valueOf("tsdb"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-tree"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-meta"), Array("name"))

    hbaseAsyncClient = new HBaseClient(s"$quorum:$port", "/hbase")

    val config = new Config(false)
    config.overrideConfig("tsd.storage.hbase.data_table", "tsdb")
    config.overrideConfig("tsd.storage.hbase.uid_table", "tsdb-uid")
    config.overrideConfig("tsd.core.auto_create_metrics", "true")
    config.overrideConfig("batchSize", "10")
    config.disableCompactions()


    tsdb = new TSDB(hbaseAsyncClient, config)
    Logger.info("Created the hbase client and the opentsdb object")

    Range(1,100).foreach(n => tsdb.addPoint("speed",System.currentTimeMillis(), n.toDouble, Map("pippotag" -> "pippovalue").asJava).joinUninterruptibly())

    Logger.info("Added data points into hbase")

    //uncomment if you want be sure that data are stored
    //scanHbase()

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
    //hbaseUtil.shutdownMiniZKCluster()
  }


  def scanHbase(): Unit = {
    val connection = hbaseUtil.getConnection
    val table: Table = connection.getTable(TableName.valueOf(Bytes.toBytes("tsdb-uid")))
    val scan: ResultScanner = table.getScanner(new Scan())
    scan.asScala.foreach(result => {
      println("Found row: " + result)
      printRow(result)

    })
  }

  private def printRow(result: Result): Unit = {
    val cells = result.rawCells()

    print( Bytes.toString(result.getRow) + " : " )
    cells.foreach{ cell =>
      val col_name = Bytes.toString(CellUtil.cloneQualifier(cell))
      val col_value = Bytes.toString(CellUtil.cloneValue(cell))
      val col_timestamp = cell.getTimestamp
      print("(%s,%s,%s) ".format(col_name, col_value, col_timestamp))

    }
    println()
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
          println(response.status)
          println(response.body)
          response.body must beEqualTo("")
        }


    }
  }
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

