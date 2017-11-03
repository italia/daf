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
import org.apache.spark.SparkConf
import org.apache.spark.opentsdb.OpenTSDBContext
import org.apache.spark.sql.SparkSession
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithServer
import shaded.org.hbase.async.HBaseClient

class OpentsdbSpec extends Specification with BeforeAfterAll{

  private val hbaseUtil = new HBaseTestingUtility()

  lazy val sparkSession: SparkSession = SparkSession.builder().master("local").getOrCreate()

  var openTSDBContext: OpenTSDBContext = _

  implicit var sparkSession: SparkSession = _

  var hbaseAsyncClient: HBaseClient = _

  var tsdb: TSDB = _

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

    streamingContext = new StreamingContext(sparkSession.sparkContext, Milliseconds(200))
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
    if (openTSDBContext.saltWidth > 0) {
      config.overrideConfig("tsd.storage.salt.width", openTSDBContext.saltWidth.toString)
      config.overrideConfig("tsd.storage.salt.buckets", openTSDBContext.saltBuckets.toString)
    }
    config.overrideConfig("batchSize", "10")
    config.disableCompactions()
    tsdb = new TSDB(hbaseAsyncClient, config)
  }

  override def afterAll(): Unit = {
    streamingContext.stop(false)
    streamingContext.awaitTermination()
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
    configure("hadoop_conf_dir" -> s"${ServiceSpec.confPath.pathAsString}").
    configure("pac4j.authenticator" -> "test").
    build()

  "The storage_manager" should {
    "read data from opentsdb" in new WithServer(app = application, port = getAvailablePort) {
      private val (doc, txtDoc, schema) = {



        ???
      }
    }
  }
}
