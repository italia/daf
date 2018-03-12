package it.teamdigitale

import com.typesafe.config.ConfigFactory
import it.teamdigitale.config.IotIngestionManagerConfig
import it.teamdigitale.managers.IotIngestionManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object Test {

  def main(args: Array[String]): Unit = {


    val sparkSession = SparkSession.builder
      .master("local")
      .appName("spark session example")
      .getOrCreate()
    val streamingContext = new StreamingContext(sparkSession.sparkContext, Seconds(10))

   val applictionConf = ConfigFactory.load()

   IotIngestionManagerSimple.run(streamingContext, sparkSession, applictionConf)

    streamingContext.start()
    streamingContext.awaitTermination()

  }
}
