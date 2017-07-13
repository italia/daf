
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
import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.common.authentication.Authentication
import org.apache.hadoop.security.UserGroupInformation
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.spark.opentsdb.OpenTSDBContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Milliseconds,StreamingContext}
import org.apache.spark.{SparkConf,SparkContext}
import org.pac4j.play.store.PlaySessionStore
import play.api.mvc.{AnyContent,Request}
import play.api.{Configuration,Environment,Mode}
import scala.annotation.tailrec
import scala.util.{Failure,Success,Try}

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package iot_ingestion_manager.yaml {
    // ----- Start of unmanaged code area for package Iot_ingestion_managerYaml
                        
  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Throw",
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

    //given a class it returns the jar (in the classpath) containing that class
    private def getJar(klass: Class[_]): String = {
      val codeSource = klass.getProtectionDomain.getCodeSource
      codeSource.getLocation.getPath
    }

    private val uberJarLocation: String = getJar(this.getClass)

    private val sparkParametersConfiguration: Configuration = configuration.getConfig("spark").getOrElse(throw new RuntimeException)

    private val conf = if (environment.mode != Mode.Test) {
      var sparkConf = new SparkConf().
        setMaster("yarn-client").
        setAppName("iot-ingestion-manager")
      sparkParametersConfiguration.entrySet.foreach(entry => {
        sparkConf = sparkConf.set(s"spark.${entry._1}", entry._2.unwrapped().asInstanceOf[String])
      })
      sparkConf
    }
    else
      new SparkConf().
        setMaster("local").
        setAppName("iot-ingestion-manager")

    private def thread(block: => Unit): Thread = {
      val thread = new Thread {
        override def run(): Unit = block
      }
      thread.start()
      thread
    }

    private def HadoopDoAsAction[T](request: Request[AnyContent])(block: => T): T = {
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

    private var sparkSession: Try[SparkSession] = Failure[SparkSession](new RuntimeException())

    private var streamingContext: Try[StreamingContext] = Failure[StreamingContext](new RuntimeException())

    private var stopFlag = true

    private var openTSDBContext: Try[OpenTSDBContext] = Failure[OpenTSDBContext](new RuntimeException())

        // ----- End of unmanaged code area for constructor Iot_ingestion_managerYaml
        val start = startAction {  _ =>  
            // ----- Start of unmanaged code area for action  Iot_ingestion_managerYaml.start
            logger.info("Begin operation 'start'")
          HadoopDoAsAction(current_request_for_startAction) {
        synchronized {
          jobThread = Some(thread {
            sparkSession match {
              case Failure(_) =>
                sparkSession = Try {
                  val ss = SparkSession.builder().config(conf).getOrCreate()
                  addClassPathJars(ss.sparkContext, getClass.getClassLoader)
                  ss
                }

                val batchDurationMillis = configuration.getLong("batch.duration").getOrElse(throw new RuntimeException)
                logger.info(s"Brokers: $batchDurationMillis")

                streamingContext = Try {
                  val ssc = sparkSession.map(ss => new StreamingContext(ss.sparkContext, Milliseconds(batchDurationMillis)))
                  ssc
                }.flatten

                val keytab = configuration.getString("opentsdb.context.keytab")
                logger.info(s"OpenTSDBContext Keytab: $keytab")

                val principal = configuration.getString("opentsdb.context.principal")
                logger.info(s"OpenTSDBContext Principal: $principal")

                val keytabLocalTempDir = configuration.getString("opentsdb.context.keytablocaltempdir")
                logger.info(s"OpenTSDBContext Keytab Local Temp Dir: $keytabLocalTempDir")

                val saltwidth = configuration.getInt("opentsdb.context.saltwidth")
                logger.info(s"OpenTSDBContext SaltWidth: $saltwidth")

                val saltbucket = configuration.getInt("opentsdb.context.saltbucket")
                logger.info(s"OpenTSDBContext SaltBucket: $saltbucket")

                saltwidth.foreach(OpenTSDBContext.saltWidth = _)
                saltbucket.foreach(OpenTSDBContext.saltBuckets = _)

                openTSDBContext = sparkSession.map(ss => {
                  val otc = new OpenTSDBContext(ss)
                  keytabLocalTempDir.foreach(otc.keytabLocalTempDir = _)
                  keytab.foreach(otc.keytab = _)
                  principal.foreach(otc.principal = _)
                  otc
                })

                val brokers = configuration.getString("bootstrap.servers").getOrElse(throw new RuntimeException)
                logger.info(s"Brokers: $brokers")

                val groupId = configuration.getString("group.id").getOrElse(throw new RuntimeException)
                logger.info(s"GroupdId: $groupId")

                val topics = Set(configuration.getString("topic").getOrElse(throw new RuntimeException))
                logger.info(s"Topics: ${topics.mkString(",")}")

                val kafkaParams = Map[String, AnyRef](
                  "bootstrap.servers" -> brokers,
                  "key.deserializer" -> classOf[ByteArrayDeserializer],
                  "value.deserializer" -> classOf[ByteArrayDeserializer],
                  "enable.auto.commit" -> (false: java.lang.Boolean),
                  "group.id" -> groupId
                )
                stopFlag = false
                streamingContext foreach {
                  ssc =>
                    logger.info("About to create the stream")
                    val inputStream = getTransformersStream(ssc, topics, kafkaParams, avroByteArrayToEvent >>>> (new testEventToDatapoint(0)))
                    openTSDBContext.foreach(_.streamWrite(inputStream))
                    logger.info("Stream created")
                    logger.info("About to start the streaming context")
                    ssc.start()
                    logger.info("Streaming context started")
                    var isStopped = false
                    while (!isStopped) {
                      logger.info("Calling awaitTerminationOrTimeout")
                      isStopped = ssc.awaitTerminationOrTimeout(1000)
                      if (isStopped)
                        logger.info("Confirmed! The streaming context is stopped. Exiting application...")
                      else
                        logger.info("The streaming context is still active. Timeout...")
                      if (!isStopped && stopFlag) {
                        logger.info("Stopping the streaming context right now")
                        ssc.stop(true, true)
                        logger.info("The streaming context is stopped!!!!!!!")
                      }
                    }
                }
              case Success(_) => ()
            }
          })
          logger.info("End operation 'start'")
          Start200("Ok")
        }
      }
            // ----- End of unmanaged code area for action  Iot_ingestion_managerYaml.start
        }
        val stop = stopAction {  _ =>  
            // ----- Start of unmanaged code area for action  Iot_ingestion_managerYaml.stop
            logger.info("Begin operation 'stop'")
          HadoopDoAsAction(current_request_for_startAction) {
        synchronized {
          sparkSession match {
            case Failure(_) => ()
            case Success(ss) =>
              logger.info("About to stop the streaming context")
              stopFlag = true
              jobThread.foreach(_.join())
              logger.info("Thread stopped")
              sparkSession = Failure[SparkSession](new RuntimeException())
          }
          logger.info("End operation 'stop'")
          Stop200("Ok")
        }
      }
            // ----- End of unmanaged code area for action  Iot_ingestion_managerYaml.stop
        }
    
    }
}
