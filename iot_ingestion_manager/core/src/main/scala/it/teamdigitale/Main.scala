package it.teamdigitale

import com.typesafe.config.ConfigFactory
import it.teamdigitale.managers.IotIngestionManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Main {

  def main(args: Array[String]): Unit = {

    val sparkSession = SparkSession.builder
      .appName("IOT INGESTION MANAGER")
      .getOrCreate()
    val streamingContext = new StreamingContext(sparkSession.sparkContext, Seconds(10))

    val applictionConf = ConfigFactory.load()

    IotIngestionManager.run(streamingContext, sparkSession, applictionConf)

    streamingContext.start()
    streamingContext.awaitTermination()

  }

}
