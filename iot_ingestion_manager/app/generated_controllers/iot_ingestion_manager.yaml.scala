
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import java.net.URLClassLoader
import java.security.PrivilegedExceptionAction
import common.Transformers.{avroByteArrayToEvent,_}
import common.TransformersStream._
import common.Util._
import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.common.authentication.Authentication
import it.gov.daf.iotingestion.common.StorableEvent
import org.apache.hadoop.conf.{Configuration=>HadoopConfiguration}
import org.apache.hadoop.security.UserGroupInformation
import org.apache.kudu.client.{CreateTableOptions,KuduException}
import org.apache.kudu.spark.kudu.KuduContext
import org.apache.spark.opentsdb.OpenTSDBContext
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.{Encoder,Encoders,SparkSession}
import org.apache.spark.streaming.{Milliseconds,StreamingContext}
import org.apache.spark.{SparkConf,SparkContext}
import org.pac4j.play.store.PlaySessionStore
import play.Logger
import play.Logger.ALogger
import play.api.mvc.{AnyContent,Request}
import play.api.{Configuration,Environment,Mode}
import scala.annotation.tailrec
import scala.collection.convert.decorateAsJava._
import scala.language.postfixOps
import scala.util.{Failure,Success,Try}
import org.apache.kudu.{Schema,Type}
import org.apache.kudu.client.CreateTableOptions
import org.apache.kudu.spark.implicits._
import org.apache.kudu.ColumnSchema
import org.apache.kudu.client.CreateTableOptions
import org.apache.kudu.ColumnSchema
import org.apache.kudu.client.CreateTableOptions

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package iot_ingestion_manager.yaml {
    // ----- Start of unmanaged code area for package Iot_ingestion_managerYaml
            
  @SuppressWarnings(
    Array(
      "org.wartremover.warts.While",
      "org.wartremover.warts.Var",
      "org.wartremover.warts.Null",
      "org.wartremover.warts.AsInstanceOf"
    )
  )
    // ----- End of unmanaged code area for package Iot_ingestion_managerYaml
    class Iot_ingestion_managerYaml @Inject() (
        // ----- Start of unmanaged code area for injections Iot_ingestion_managerYaml
                                             val environment: Environment,
                                             val configuration: Configuration,
                                             val playSessionStore: PlaySessionStore,
        // ----- End of unmanaged code area for injections Iot_ingestion_managerYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Iot_ingestion_managerYamlBase {
        // ----- Start of unmanaged code area for constructor Iot_ingestion_managerYaml

    implicit private val alogger: ALogger = Logger.of(this.getClass.getCanonicalName)

    Authentication(configuration, playSessionStore)

    @tailrec
    private def addClassPathJars(sparkContext: SparkContext, classLoader: ClassLoader): Unit = {
      classLoader match {
        case urlClassLoader: URLClassLoader =>
          urlClassLoader.getURLs.foreach { classPathUrl =>
            if (classPathUrl.toExternalForm.endsWith(".jar") && !classPathUrl.toExternalForm.contains("test-interface")) {
              sparkContext.addJar(classPathUrl.toExternalForm)
            }
          }
        case _ =>
      }
      if (classLoader.getParent != null) {
        addClassPathJars(sparkContext, classLoader.getParent)
      }
    }

    private val conf = if (environment.mode != Mode.Test) {
      var sparkConf = new SparkConf().
        setMaster("yarn-client").
        setAppName("iot-ingestion-manager")
      configuration.getConfig("spark").foreach(_.entrySet.foreach(entry => {
        sparkConf = sparkConf.set(s"spark.${entry._1}", entry._2.unwrapped().asInstanceOf[String])
      }))
      sparkConf
    }
    else
      new SparkConf().
        setMaster("local[8]").
        setAppName("iot-ingestion-manager")

    private def thread(block: => Unit): Thread = {
      val thread = new Thread {
        override def run(): Unit = block
      }
      thread.start()
      thread
    }

    private def HadoopDoAsAction[T](proxyUser: UserGroupInformation, request: Request[AnyContent])(block: => T): T = {
      val profiles = Authentication.getProfiles(request)
      val user = profiles.headOption.map(_.getId).getOrElse("anonymous")
      val ugi = UserGroupInformation.createProxyUser(user, proxyUser)
      ugi.doAs(new PrivilegedExceptionAction[T]() {
        override def run: T = block
      })
    }

    UserGroupInformation.loginUserFromSubject(null)

    private val proxyUser = UserGroupInformation.getCurrentUser

    private var jobThread: Option[Thread] = None

    private var sparkSession: Try[SparkSession] = InitializeFailure[SparkSession]

    private var streamingContext: Try[StreamingContext] = InitializeFailure[StreamingContext]

    private var stopFlag = true

    private var openTSDBContext: Try[OpenTSDBContext] = InitializeFailure[OpenTSDBContext]

    private def createKuduOffsetTable(kuduContext: KuduContext, kuduOffsetsTableName: String, kuduOffsetsTableNumberOfBuckets: Int): Unit = {
      val kuduClient = kuduContext.syncClient
      if (!kuduClient.tableExists(kuduOffsetsTableName)) {
        import org.apache.kudu.ColumnSchema
        import org.apache.kudu.client.CreateTableOptions

        val columns = List(
          new ColumnSchema.ColumnSchemaBuilder("topic", Type.STRING).key(true).build,
          new ColumnSchema.ColumnSchemaBuilder("groupId", Type.STRING).key(true).build,
          new ColumnSchema.ColumnSchemaBuilder("partitions", Type.STRING).build,
          new ColumnSchema.ColumnSchemaBuilder("offsets", Type.STRING).build
        )
        val schema = new Schema(columns.asJava)
        val table = kuduClient.createTable(
          kuduOffsetsTableName,
          schema,
          new CreateTableOptions().addHashPartitions(List("topic", "groupId").asJava, kuduOffsetsTableNumberOfBuckets))
      }
    }

    private def createKuduEventsTable(kuduContext: KuduContext, kuduEventsTableName: String, kuduEventsTableNumberOfBuckets: Int): Unit = {
      if (!kuduContext.tableExists(kuduEventsTableName)) {
        try {
          val schema = Encoders.product[StorableEvent].schema
          val _ = kuduContext.createTable(
            kuduEventsTableName,
            schema,
            Seq("id", "ts"),
            new CreateTableOptions().
              setRangePartitionColumns(List("ts").asJava).
              addHashPartitions(List("id").asJava, kuduEventsTableNumberOfBuckets)
          )
        } catch {
          case ex: KuduException if ex.getStatus.isAlreadyPresent =>
        }
      }
    }

        // ----- End of unmanaged code area for constructor Iot_ingestion_managerYaml
        val start = startAction {  _ =>  
            // ----- Start of unmanaged code area for action  Iot_ingestion_managerYaml.start
            alogger.info("Begin operation 'start'")
      HadoopDoAsAction(proxyUser, current_request_for_startAction) {
        synchronized {
          jobThread = Some(thread {
            sparkSession match {
              case Failure(_) =>
                sparkSession = Try {
                  val ss = SparkSession.builder().config(conf).getOrCreate()
                  addClassPathJars(ss.sparkContext, getClass.getClassLoader)
                  ss
                }

                val batchDurationMillis = configuration.getLongOrException("batch.duration")
                alogger.info(s"Batch Duration Millis: $batchDurationMillis")

                streamingContext = Try {
                  val ssc = sparkSession.map(ss => new StreamingContext(ss.sparkContext, Milliseconds(batchDurationMillis)))
                  ssc
                }.flatten

                val keytab = configuration.getString("opentsdb.context.keytab")
                alogger.info(s"OpenTSDBContext Keytab: $keytab")

                val principal = configuration.getString("opentsdb.context.principal")
                alogger.info(s"OpenTSDBContext Principal: $principal")

                val keytabLocalTempDir = configuration.getString("opentsdb.context.keytablocaltempdir")
                alogger.info(s"OpenTSDBContext Keytab Local Temp Dir: $keytabLocalTempDir")

                val saltwidth = configuration.getInt("opentsdb.context.saltwidth")
                alogger.info(s"OpenTSDBContext SaltWidth: $saltwidth")

                val saltbucket = configuration.getInt("opentsdb.context.saltbucket")
                alogger.info(s"OpenTSDBContext SaltBucket: $saltbucket")

                saltwidth.foreach(OpenTSDBContext.saltWidth = _)
                saltbucket.foreach(OpenTSDBContext.saltBuckets = _)

                openTSDBContext = sparkSession.map(ss => {
                  val otc = new OpenTSDBContext(ss)
                  keytabLocalTempDir.foreach(otc.keytabLocalTempDir = _)
                  keytab.foreach(otc.keytab = _)
                  principal.foreach(otc.principal = _)
                  otc
                })

                val brokers = configuration.getStringOrException("bootstrap.servers")
                alogger.info(s"Brokers: $brokers")

                val groupId = configuration.getStringOrException("group.id")
                alogger.info(s"GroupdId: $groupId")

                val topic = configuration.getStringOrException("topic")
                alogger.info(s"Topics: $topic")

                val kafkaZkQuorum = configuration.getStringOrException("kafka.zookeeper.quorum")
                alogger.info(s"Kafka Zk Quorum: $kafkaZkQuorum")

                val kafkaZkRootDir = configuration.getString("kafka.zookeeper.root")
                alogger.info(s"Kafka Zk Root Dir: $kafkaZkRootDir")

                val kuduMasterAddresses = configuration.getStringOrException("kudu.master.addresses")

                alogger.info(s"Kudu Master Addresses: $kuduMasterAddresses")

                val kuduOffsetsTableName = configuration.getStringOrException("kudu.offsets.table.name")

                alogger.info(s"Kudu Offsets Table Name: $kuduOffsetsTableName")

                val kuduEventsTableName = configuration.getStringOrException("kudu.events.table.name")

                alogger.info(s"Kudu Events Table Name: $kuduEventsTableName")

                val kuduOffsetsTableNumberOfBuckets = configuration.getIntOrException("kudu.offsets.table.numberOfBuckets")

                alogger.info(s"Kudu Offsets Number Of Buckets: $kuduOffsetsTableNumberOfBuckets")

                val kuduEventsTableNumberOfBuckets = configuration.getIntOrException("kudu.events.table.numberOfBuckets")

                alogger.info(s"Kudu Events Number Of Buckets: $kuduEventsTableNumberOfBuckets")

                stopFlag = false
                streamingContext foreach {
                  ssc =>
                    alogger.info("About to create the stream")

                    val kuduContext = new KuduContext(kuduMasterAddresses, ssc.sparkContext)

                    sparkSession foreach {
                      ss =>
                        createKuduEventsTable(kuduContext, kuduEventsTableName, kuduEventsTableNumberOfBuckets)
                        createKuduOffsetTable(kuduContext, kuduOffsetsTableName, kuduOffsetsTableNumberOfBuckets)
                    }

                    val inputStream = getTransformersStream(ssc, kuduContext, kafkaZkQuorum, kafkaZkRootDir, kuduOffsetsTableName, brokers, topic, groupId, avroByteArrayToEvent)
                    inputStream match {
                      case Success(stream) =>
                        sparkSession foreach {
                          implicit ss =>

                            implicit val storableEventEncoder: Encoder[StorableEvent] = ExpressionEncoder()

                            val dataPoints = stream.
                              applyTransform(eventToStorableEvent).
                              transform {
                                source =>
                                  convertDataFrameToRDD[StorableEvent](kuduContext.insertAndReturn(convertRDDtoDataFrame[StorableEvent](source), kuduEventsTableName))
                              }.
                              applyTransform(storableEventToDatapoint)

                            openTSDBContext.foreach(_.streamWrite(dataPoints))

                            alogger.info("Stream created")
                        }
                      case Failure(ex) => alogger.error(s"Failed in creating the stream: ${ex.getCause}")
                    }
                    alogger.info("About to start the streaming context")
                    ssc.start()
                    alogger.info("Streaming context started")
                    var isStopped = false
                    while (!isStopped) {
                      alogger.info("Calling awaitTerminationOrTimeout")
                      isStopped = ssc.awaitTerminationOrTimeout(1000)
                      if (isStopped)
                        alogger.info("Confirmed! The streaming context is stopped. Exiting application...")
                      else
                        alogger.info("The streaming context is still active. Timeout...")
                      if (!isStopped && stopFlag) {
                        alogger.info("Stopping the streaming context right now")
                        ssc.stop(stopSparkContext = true, stopGracefully = false)
                        alogger.info("The streaming context is stopped!!!!!!!")
                      }
                    }
                }
              case Success(_) => ()
            }
          })
          alogger.info("End operation 'start'")
          Start200("Ok")
        }
      }
            // ----- End of unmanaged code area for action  Iot_ingestion_managerYaml.start
        }
        val stop = stopAction {  _ =>  
            // ----- Start of unmanaged code area for action  Iot_ingestion_managerYaml.stop
            alogger.info("Begin operation 'stop'")
      HadoopDoAsAction(proxyUser, current_request_for_startAction) {
        synchronized {
          sparkSession match {
            case Failure(_) => ()
            case Success(_) =>
              alogger.info("About to stop the streaming context")
              stopFlag = true
              jobThread.foreach(_.join())
              alogger.info("Thread stopped")
              sparkSession = InitializeFailure[SparkSession]
          }
          alogger.info("End operation 'stop'")
          Stop200("Ok")
        }
      }
            // ----- End of unmanaged code area for action  Iot_ingestion_managerYaml.stop
        }
    
    }
}
