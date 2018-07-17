package it.teamdigitale.managers

import com.typesafe.config.Config
import it.gov.daf.iotingestion.event.Event
import it.teamdigitale.EventModel._
import it.teamdigitale.config.IotIngestionManagerConfig
import it.teamdigitale.config.IotIngestionManagerConfig.{HdfsConfig, KafkaConfig, KuduConfig}
import it.teamdigitale.events.SerializerDeserializer
import it.teamdigitale.storage.{HdfsEventsHandler, KuduEventsHandler}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.logging.log4j.LogManager
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.util.{Failure, Try}


object IotIngestionManager  {
  val alogger = LogManager.getLogger(this.getClass)

  def run(
           streamingContext : StreamingContext,
           sc: SparkSession,
           kuduContext: KuduContext,
           kafkaconfig: KafkaConfig,
           kuduConfig: KuduConfig,
           hdfsConfig: HdfsConfig): Unit = {

    KuduEventsHandler.getOrCreateTable(kuduContext, kuduConfig)


    val kafkaParams = kafkaconfig.getParams()

    alogger.info(s"Starting Streaming without information on kafka partitioning")
    val stream = KafkaUtils.createDirectStream[Array[Byte], Array[Byte]](streamingContext, PreferConsistent, ConsumerStrategies.Subscribe[Array[Byte], Array[Byte]](Set(kafkaconfig.topic), kafkaParams))

    //daf.stream.print(10)

    stream.foreachRDD{ rdd =>

      val tryIterator = rdd
        .map(x => SerializerDeserializer.deserialize(x.value()))
        .map(event => EventToStorableEvent(event))

      tryIterator.filter(x => x.isFailure).take(10).foreach{case Failure(error) => alogger.error(error.getMessage)}

      val cachedRDD = tryIterator.flatMap(_.toOption).cache()
      val tryMetricsRDD = cachedRDD.filter(_.event_type_id != 2).map(se => EventToKuduEvent(se))

      tryMetricsRDD.filter(x => x.isFailure).take(10).foreach{case Failure(error) => alogger.error(error.getMessage)}

      val metricsDF = sc.createDataFrame(tryMetricsRDD.flatMap(_.toOption))
      KuduEventsHandler.write(metricsDF,kuduContext, kuduConfig)

      val tryGenericEventsRDD = cachedRDD.map(e => EventToHdfsEvent(e))
      tryGenericEventsRDD.filter(x => x.isFailure).take(10).foreach{case Failure(error) => alogger.error(error.getMessage)}

      val genericEventsRDD = tryGenericEventsRDD.flatMap(_.toOption)
      val genericEventsDF = sc.createDataFrame(genericEventsRDD)
      HdfsEventsHandler.write(genericEventsDF, hdfsConfig)

      cachedRDD.unpersist(false)

    }
  }


    /**
    * Start the kafka daf.stream and store metric events (i.e. events having event_type_id != 2)
    * on Kudu and generic events (i.e. events having event_type_id == 2) on HDFS
    */
  def run(
           streamingContext : StreamingContext,
           sc: SparkSession,
           configuration: Config): Unit  = {

    val kafkaconfig = IotIngestionManagerConfig.getKafkaConfig(configuration)
    val kuduConfig = IotIngestionManagerConfig.getKuduConfig(configuration)
    val hdfsConfig = IotIngestionManagerConfig.getHdfSConfig(configuration)
    val kuduContext = new KuduContext(kuduConfig.masterAdresses, sc.sparkContext)

    run(streamingContext, sc, kuduContext, kafkaconfig, kuduConfig, hdfsConfig)

  }



}
