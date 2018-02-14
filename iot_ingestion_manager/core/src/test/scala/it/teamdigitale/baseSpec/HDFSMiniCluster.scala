package it.teamdigitale.baseSpec

import better.files.File
import org.slf4j.{Logger, LoggerFactory}
import org.apache.hadoop.hdfs.HdfsConfiguration
import org.apache.hadoop.test.PathUtils
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.{HdfsConfiguration, MiniDFSCluster}
import better.files._
import scala.util.{Failure, Try}

class HDFSMiniCluster extends AutoCloseable {
  val alogger: Logger = LoggerFactory.getLogger(this.getClass)

  var hdfsCluster: Try[MiniDFSCluster] = Failure[MiniDFSCluster](new Exception)
  var fileSystem: Try[FileSystem] = Failure[FileSystem](new Exception)

  val (testDataPath, confPath) = {
    val testDataPath = s"${PathUtils.getTestDir(classOf[HDFSMiniCluster]).getCanonicalPath}/MiniCluster"
    val confPath = s"$testDataPath/conf"
    (
      testDataPath.toFile.createIfNotExists(asDirectory = true, createParents = false),
      confPath.toFile.createIfNotExists(asDirectory = true, createParents = false)
    )
  }


  def start(): Unit = {

    alogger.info("Starting HDFS mini cluster")
    val conf = new HdfsConfiguration()
    conf.setBoolean("dfs.permissions", true)
    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA)

    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, testDataPath.pathAsString)
    //FileUtil.fullyDelete(testDataPath.toJava)

    conf.set(s"hadoop.proxyuser.${System.getProperties.get("user.name")}.groups", "*")
    conf.set(s"hadoop.proxyuser.${System.getProperties.get("user.name")}.hosts", "*")

    val builder = new MiniDFSCluster.Builder(conf)
    hdfsCluster = Try(builder.build())
    fileSystem = hdfsCluster.map(_.getFileSystem)
    fileSystem.foreach(fs => {
      val confFile: File = confPath / "hdfs-site.xml"
      for {os <- confFile.newOutputStream.autoClosed} fs.getConf.writeXml(os)
    })

  }


  override def close() = {
    alogger.info("Stopping HDFS mini cluster")
    hdfsCluster.foreach(_.shutdown(true))
    val _ = testDataPath.parent.parent.delete(true)
  }
}
