
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
import common.Transformers._
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
      "org.wartremover.warts.Null"
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

    private val conf = if (environment.mode != Mode.Test)
      new SparkConf().
        setMaster("yarn-client").
        setAppName("iot-ingestion-manager").
        setJars(List(uberJarLocation)).
        set("spark.yarn.jars", "local:/opt/cloudera/parcels/SPARK2/lib/spark2/jars/*").
        set("spark.serializer", "org.apache.spark.serializer.KryoSerializer").
        set("spark.io.compression.codec", "lzf").
        set("spark.speculation", "false").
        set("spark.shuffle.manager", "sort").
        set("spark.shuffle.service.enabled", "true").
        set("spark.dynamicAllocation.enabled", "true").
        set("spark.dynamicAllocation.minExecutors", "8").
        set("spark.dynamicAllocation.initialExecutors", "8").
        set("spark.executor.cores", Integer.toString(1)).
        set("spark.executor.memory", "2048m").
        set("spark.executor.extraJavaOptions", "-Djava.security.auth.login.config=/tmp/jaas.conf")
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

    private var openTSDBContext: Try[OpenTSDBContext] = Failure[OpenTSDBContext](new RuntimeException())

        // ----- End of unmanaged code area for constructor Iot_ingestion_managerYaml
        val start = startAction {  _ =>  
            // ----- Start of unmanaged code area for action  Iot_ingestion_managerYaml.start
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

                streamingContext = Try {
                  val ssc = sparkSession.map(ss => new StreamingContext(ss.sparkContext, Milliseconds(500)))
                  ssc
                }.flatten

                openTSDBContext = sparkSession.map(new OpenTSDBContext(_))

                val brokers = configuration.getString("bootstrap.servers").getOrElse(throw new RuntimeException)

                println(brokers)

                val groupId = configuration.getString("group.id").getOrElse(throw new RuntimeException)

                println(groupId)

                val topics = Set(configuration.getString("topic").getOrElse(throw new RuntimeException))

                println(topics)

                val kafkaParams = Map[String, AnyRef](
                  "bootstrap.servers" -> brokers,
                  "key.deserializer" -> classOf[ByteArrayDeserializer],
                  "value.deserializer" -> classOf[ByteArrayDeserializer],
                  "enable.auto.commit" -> (false: java.lang.Boolean),
                  "group.id" -> groupId
                )

                streamingContext foreach {
                  ssc =>
                    println("About to create the stream")
                    getTransformersStream(ssc, topics, kafkaParams, avroByteArrayToEvent >>>> eventToDatapoint).print(10)
                    println("Stream created")
                    println("About to start the context")
                    ssc.start()
                    println("Context started")
                }

                while (!sparkSession.getOrElse(throw new RuntimeException).sparkContext.isStopped)
                  Thread.sleep(100)
              case Success(_) => ()
            }
          })
          Start200("Ok")
        }
      }
            // ----- End of unmanaged code area for action  Iot_ingestion_managerYaml.start
        }
        val stop = stopAction {  _ =>  
            // ----- Start of unmanaged code area for action  Iot_ingestion_managerYaml.stop
            HadoopDoAsAction(current_request_for_startAction) {
        synchronized {
          sparkSession match {
            case Failure(_) => ()
            case Success(ss) =>
              streamingContext.foreach(_.stop(false))
              streamingContext.foreach(_.awaitTermination())
              ss.stop()
              jobThread.foreach(_.join())
              sparkSession = Failure[SparkSession](new RuntimeException())
          }
          Stop200("Ok")
        }
      }
            // ----- End of unmanaged code area for action  Iot_ingestion_managerYaml.stop
        }
    
    }
}
