package it.teamdigitale.miniclusters

import org.apache.kudu.client.{KuduClient, MiniKuduCluster}
import org.apache.kudu.client.KuduClient.KuduClientBuilder
import org.apache.kudu.client.MiniKuduCluster.MiniKuduClusterBuilder
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.apache.logging.log4j.LogManager

class KuduMiniCluster extends AutoCloseable {
  val alogger = LogManager.getLogger(this.getClass)

  var kuduMiniCluster: MiniKuduCluster = _
  var kuduClient: KuduClient = _
  var kuduContext: KuduContext = _
  var sparkSession = SparkSession.builder().appName(s"test-${System.currentTimeMillis()}").master("local[*]").getOrCreate()

  def start() {
    alogger.info("Starting KUDU mini cluster")

    System.setProperty(
      "binDir",
      s"${System.getProperty("user.dir")}/src/test/kudu_executables/${sun.awt.OSInfo.getOSType().toString.toLowerCase}"
    )


    kuduMiniCluster = new MiniKuduClusterBuilder()
      .numMasters(1)
      .numTservers(3)
      .build()

    val envMap = Map[String, String](("Xmx", "512m"))

    kuduClient = new KuduClientBuilder(kuduMiniCluster.getMasterAddresses).build()
    assert(kuduMiniCluster.waitForTabletServers(1))

    kuduContext = new KuduContext(kuduMiniCluster.getMasterAddresses, sparkSession.sparkContext)

  }

  override def close() {
    alogger.info("Ending KUDU mini cluster")
    kuduClient.shutdown()
    kuduMiniCluster.shutdown()
    sparkSession.close()
  }
}

  object KuduMiniCluster {

    def main(args: Array[String]): Unit = {

      try {
        val kudu = new KuduMiniCluster()
        kudu.start()

        println(s"MASTER KUDU ${kudu.kuduMiniCluster.getMasterAddresses}")
        while(true){

        }
      }
    }


  }

