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
  implicit private val alogger = LogManager.getLogger(this.getClass)

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

    stream.print(10)

    stream.foreachRDD{ rdd =>

      val tryEvents = rdd.map(x => SerializerDeserializer.deserialize(x.value()))
      val (metricsRDD, genericEventsRDD) = filterEvents(tryEvents)

      val metricsDF = sc.createDataFrame(metricsRDD)
      kuduContext.insertIgnoreRows(metricsDF, kuduConfig.eventsTableName)

      val genericEventsDF = sc.createDataFrame(genericEventsRDD)
      HdfsEventsHandler.write(genericEventsDF, hdfsConfig)

    }
  }

    /**
    * Start the kafka stream and store metric events (i.e. events having event_type_id != 2)
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

  /**
    *
    * @param rdd input rdd of Try[Event] returned by Kafka
    * @return a tuple (r1, r2), where r1 is an RDD[StorableEvent] and contains data that will be stored in KUDU; r2 is an RDD[StorableGenericEvent] and data that will be stored in HDFS
    */
  def filterEvents(rdd:  RDD[Try[Event]]): (RDD[StorableEvent], RDD[StorableGenericEvent]) = {

    val tryIterator = rdd
      .map(event => EventToStorableEvent(event))

    tryIterator.filter(x => x.isFailure).take(10).foreach{case Failure(error) => alogger.error(error.getMessage)}

    val cachedRDD = tryIterator.flatMap(_.toOption).cache()
    val metricsRDD = cachedRDD.filter(_.event_type_id != 2)

    // filter generic events
    val genericEventsRDD = cachedRDD.filter(_.event_type_id == 2)
      .map(e => EventToStorableGenericEvent(e))

    (metricsRDD, genericEventsRDD)
  }


}
