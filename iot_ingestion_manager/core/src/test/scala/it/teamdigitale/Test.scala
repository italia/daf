package it.teamdigitale

import com.typesafe.config.ConfigFactory
import it.teamdigitale.managers.IotIngestionManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

object Test {

  def main(args: Array[String]): Unit = {


    val sparkSession = SparkSession.builder
      .master("local")
      .appName("spark session example")
      .getOrCreate()

   val applictionConf = ConfigFactory.load()


   //val iotManger = new IotIngestionManager(sparkSession, applictionConf)
   //println(iotManger.kafkaconfig)
  }
}
