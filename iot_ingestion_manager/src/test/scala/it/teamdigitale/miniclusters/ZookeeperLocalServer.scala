package it.teamdigitale.miniclusters

import java.io.File
import java.net.InetSocketAddress

import org.apache.commons.io.FileUtils
import org.apache.zookeeper.server.{ServerCnxnFactory, ZooKeeperServer}

class ZookeeperLocalServer(port: Int) {

  var zkServer: Option[ServerCnxnFactory] = None

  def start(): Unit = {
    if (zkServer.isEmpty) {

      val dataDirectory = System.getProperty("java.io.tmpdir")
      val dir = new File(dataDirectory, "zookeeper")
      println(dir.toString)
      if (dir.exists())
        FileUtils.deleteDirectory(dir)

      try {
        val tickTime = 5000
        val server = new ZooKeeperServer(dir.getAbsoluteFile, dir.getAbsoluteFile, tickTime)
        val factory = ServerCnxnFactory.createFactory
        factory.configure(new InetSocketAddress("0.0.0.0", port), 1024)
        factory.startup(server)
        println("ZOOKEEPER server up!!")
        zkServer = Some(factory)

      } catch {
        case ex: Exception => System.err.println(s"Error in zookeeper server: ${ex.printStackTrace()}")
      } finally { dir.deleteOnExit() }
    } else println("ZOOKEEPER is already up")
  }

  def stop() = {
    if (zkServer.isDefined) {
      zkServer.get.shutdown()
    }
    println("ZOOKEEPER server stopped")
  }
}
