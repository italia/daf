package it.teamdigitale.instances

import org.apache.spark.sql.SparkSession

trait SparkSessionInstance {

  protected lazy val sparkSession = SparkSession.builder()
    .appName(s"TestSpark_${System.currentTimeMillis()}")
    .master(s"local[*]")
    .config("spark.executor.cores", "2")
    .config("spark.dynamicAllocation.enabled", "true")
    .config("spark.dynamicAllocation.minExecutors", "2")
    .config("spark.dynamicAllocation.initialExecutors", "2")
    .getOrCreate()

  protected lazy val sparkContext = sparkSession.sparkContext

  protected lazy val sqlContext = sparkSession.sqlContext

}