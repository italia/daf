package it.teamdigitale.baseSpec

import better.files.File
import it.teamdigitale.{HDFSController, HDFSControllerSpec}
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.{HdfsConfiguration, MiniDFSCluster}
import org.apache.hadoop.test.PathUtils
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.{Logger, LoggerFactory}
import better.files._
import it.teamdigitale.baseSpec.DataFrameClasses.{Address, Person}

import scala.util.{Failure, Random, Try}

abstract class HDFSbase extends FlatSpec with Matchers with BeforeAndAfterAll {

  var miniCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)

  var fileSystem: Try[FileSystem] = Failure[FileSystem](new Exception)

  val sparkSession: SparkSession = SparkSession.builder().master("local").getOrCreate()

  val alogger: Logger = LoggerFactory.getLogger(this.getClass)

  val (testDataPath, confPath) = {
    val testDataPath = s"${PathUtils.getTestDir(classOf[HDFSControllerSpec]).getCanonicalPath}/MiniCluster"
    val confPath = s"$testDataPath/conf"
    (
      testDataPath.toFile.createIfNotExists(asDirectory = true, createParents = false),
      confPath.toFile.createIfNotExists(asDirectory = true, createParents = false)
    )
  }

  def pathAvro = "opendata/test.avro"
  def pathParquet = "opendata/test.parquet"
  def pathCsv = "opendata/test.csv"

  def getSparkSession = sparkSession

  override def beforeAll(): Unit = {

    val conf = new HdfsConfiguration()
    conf.setBoolean("dfs.permissions", true)
    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA)

    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, testDataPath.pathAsString)
    //FileUtil.fullyDelete(testDataPath.toJava)

    conf.set(s"hadoop.proxyuser.${System.getProperties.get("user.name")}.groups", "*")
    conf.set(s"hadoop.proxyuser.${System.getProperties.get("user.name")}.hosts", "*")

    val builder = new MiniDFSCluster.Builder(conf)
    miniCluster = Try(builder.build())
    fileSystem = miniCluster.map(_.getFileSystem)
    fileSystem.foreach(fs => {
      val confFile: File = confPath / "hdfs-site.xml"
      for {os <- confFile.newOutputStream.autoClosed} fs.getConf.writeXml(os)
    })

    writeDf()
  }


  override def afterAll(): Unit = {
    miniCluster.foreach(_.shutdown(true))
    val _ = testDataPath.parent.parent.delete(true)
    sparkSession.stop()
  }

  /**
    * It generate a trivial dataframe and store three files (i.e. test.avro, test.parquet, test.csv) in path
    *
    */
  private def writeDf(): Unit = {
    import sparkSession.implicits._


    alogger.info(s"TestDataPath ${testDataPath.toJava.getAbsolutePath}")
    alogger.info(s"ConfPath ${confPath.toJava.getAbsolutePath}")
    val persons = (1 to 10).map(i => Person(s"Andy$i", Random.nextInt(85), Address("Via Ciccio Cappuccio")))
    val caseClassDS = persons.toDS()

    caseClassDS.write.format("parquet").mode(SaveMode.Overwrite).save(pathParquet)
    caseClassDS.write.format("com.databricks.spark.avro").mode(SaveMode.Overwrite).save(pathAvro)
    caseClassDS.toDF.select("name", "age").write.format("csv").mode(SaveMode.Overwrite).save(pathCsv)

  }
}

object DataFrameClasses {

  final case class Address(street: String)

  final case class Person(name: String, age: Int, address: Address)
}
