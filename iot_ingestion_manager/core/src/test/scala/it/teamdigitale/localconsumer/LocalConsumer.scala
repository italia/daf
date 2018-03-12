package it.teamdigitale.localconsumer

import it.teamdigitale.miniclusters.{HDFSMiniCluster, KuduMiniCluster}
import it.teamdigitale.config.IotIngestionManagerConfig.{HdfsConfig, KafkaConfig, KuduConfig}
import it.teamdigitale.managers.{IotIngestionManager, IotIngestionManagerSpec}
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.spark.sql.SparkSession

object LocalConsumer {

  val kuducluster = new KuduMiniCluster()
  val hdfscluster = new HDFSMiniCluster()

  def main(args: Array[String]): Unit = {

    try {
      kuducluster.start()
      hdfscluster.start()

      val kuduConfig = KuduConfig(kuducluster.kuduMiniCluster.getMasterAddresses, "TestEvents", 2)
      val hdfsConfig = HdfsConfig("./hdfsProva")
      val kafkaconfig = KafkaConfig(s"localhost:9092", "groupTest", "kafkaTopic")

      IotIngestionManager.run(kuducluster.sparkSession, kuducluster.kuduContext, kafkaconfig,
        kuduConfig, hdfsConfig)
    }
  }

}
