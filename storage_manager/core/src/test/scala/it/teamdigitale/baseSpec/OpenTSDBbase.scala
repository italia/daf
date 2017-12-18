package it.teamdigitale.baseSpec

import net.opentsdb.core.TSDB
import net.opentsdb.utils.Config
import org.apache.commons.io.FileUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hbase.client.{Result, ResultScanner, Scan, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, HBaseTestingUtility, TableName}
import org.apache.hadoop.hdfs.MiniDFSCluster
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.{Logger, LoggerFactory}
import shaded.org.hbase.async.HBaseClient

import scala.collection.convert.decorateAsJava._
import scala.collection.convert.decorateAsScala._
import scala.util.{Failure, Try}

abstract class OpenTSDBbase extends FlatSpec with Matchers with BeforeAndAfterAll {

  var miniCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)

  var fileSystem: Try[FileSystem] = Failure[FileSystem](new Exception)

  var hbaseUtil: HBaseTestingUtility = new HBaseTestingUtility()

  var hbaseAsyncClient: HBaseClient = _

  var tsdb: TSDB = _

  var baseConf: Configuration = _

  private val alogger: Logger = LoggerFactory.getLogger(this.getClass)

  def metric = "speed"

  var sparkSession: SparkSession = _

  def getSparkSession = sparkSession

  override def beforeAll(): Unit = {

    val dataDirectory = System.getProperty("java.io.tmpdir")

    val dir = new java.io.File(dataDirectory, "zookeeper")

    alogger.info(s"Zookeeper tmp dir: ${dir.toString}")
    if (dir.exists()) {
      alogger.info(s"Deleting dir ${dir.toString}")
      FileUtils.deleteDirectory(dir)
    }

    baseConf = hbaseUtil.getConfiguration
    hbaseUtil.getConfiguration.set("test.hbase.zookeeper.property.clientPort", "2181")

    //baseConf.iterator().asScala.foreach(println)
    println("Starting the HBase minicluster")
    hbaseUtil.startMiniCluster(1)
    println("Started the HBase minicluster")

    sparkSession = SparkSession.builder().master("local").getOrCreate()
    HBaseConfiguration.merge(sparkSession.sparkContext.hadoopConfiguration, baseConf)

    val quorum = baseConf.get("hbase.zookeeper.quorum")
    val port = baseConf.get("hbase.zookeeper.property.clientPort")

    hbaseUtil.createTable(TableName.valueOf("tsdb-uid"), Array("id", "name"))
    hbaseUtil.createTable(TableName.valueOf("tsdb"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-tree"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-meta"), Array("name"))

    alogger.info("Starting HBASE")
    hbaseAsyncClient = new HBaseClient(s"$quorum:$port", "/hbase")

    val config = new Config(false)
    config.overrideConfig("tsd.storage.hbase.data_table", "tsdb")
    config.overrideConfig("tsd.storage.hbase.uid_table", "tsdb-uid")
    config.overrideConfig("tsd.core.auto_create_metrics", "true")
    config.overrideConfig("batchSize", "10")
    config.disableCompactions()

    tsdb = new TSDB(hbaseAsyncClient, config)
    alogger.info("Created the hbase client and the opentsdb object")

    Range(1, 100).foreach(n => tsdb.addPoint(metric, System.currentTimeMillis(), n.toDouble, Map("pippotag" -> "pippovalue").asJava).joinUninterruptibly())

    alogger.info("Added data points into hbase")

    //uncomment if you want be sure that data are correctly stored
    //scanHbase()

  }

  override def afterAll(): Unit = {
    sparkSession.stop()
    tsdb.shutdown()
    hbaseUtil.deleteTable("tsdb-uid")
    hbaseUtil.deleteTable("tsdb")
    hbaseUtil.deleteTable("tsdb-tree")
    hbaseUtil.deleteTable("tsdb-meta")
    hbaseUtil.shutdownMiniCluster()
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

    print(Bytes.toString(result.getRow) + " : ")
    cells.foreach { cell =>
      val col_name = Bytes.toString(CellUtil.cloneQualifier(cell))
      val col_value = Bytes.toString(CellUtil.cloneValue(cell))
      val col_timestamp = cell.getTimestamp
      print("(%s,%s,%s) ".format(col_name, col_value, col_timestamp))

    }
    println()
  }

}
