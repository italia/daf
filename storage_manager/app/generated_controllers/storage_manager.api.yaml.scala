
import java.net.URI
import javax.inject._

import org.apache.spark.sql.SparkSession
import play.api.i18n.MessagesApi
import play.api.inject.{ApplicationLifecycle, ConfigurationProvider}

import scala.language.reflectiveCalls

/**
  * This controller is re-generated after each change in the specification.
  * Please only place your hand-written code between appropriate comments in the body of the controller.
  */

package storage_manager.api.yaml {

  import org.apache.spark.sql.streaming.StreamingQuery
  import org.apache.spark.sql.{ForeachWriter, Row}

  import scala.concurrent.ExecutionContext

  // ----- Start of unmanaged code area for package Storage_managerApiYaml
  @SuppressWarnings(Array("org.wartremover.warts.While", "org.wartremover.warts.Var", "org.wartremover.warts.ImplicitParameter"))
  // ----- End of unmanaged code area for package Storage_managerApiYaml
  class Storage_managerApiYaml @Inject()(
                                          // ----- Start of unmanaged code area for injections Storage_managerApiYaml
                                          implicit ec: ExecutionContext,
                                          // ----- End of unmanaged code area for injections Storage_managerApiYaml
                                          val messagesApi: MessagesApi,
                                          lifecycle: ApplicationLifecycle,
                                          config: ConfigurationProvider
                                        ) extends Storage_managerApiYamlBase {
    // ----- Start of unmanaged code area for constructor Storage_managerApiYaml
    val sparkSession: SparkSession = SparkSession.builder().master("local").getOrCreate()
    // ----- End of unmanaged code area for constructor Storage_managerApiYaml
    val getdataset = getdatasetAction { input: (String, String) =>
      val (uri, format) = input
      // ----- Start of unmanaged code area for action  Storage_managerApiYaml.getdataset
      val datasetURI = new URI(uri)
      val locationURI = new URI(datasetURI.getSchemeSpecificPart)
      val locationScheme = locationURI.getScheme
      val actualFormat = format match {
        case "avro" => "com.databricks.spark.avro"
        case format: String => format
      }
      val _ = locationScheme match {
        case "hdfs" =>
          val location = locationURI.getSchemeSpecificPart
          val df = sparkSession.read.format(actualFormat).load(location)
          val sdf = sparkSession.readStream.format(actualFormat).schema(df.schema).load(location)
          var closed = false
          val query = sdf.writeStream.foreach(new ForeachWriter[org.apache.spark.sql.Row] {
            override def open(partitionId: Long, version: Long): Boolean = true

            override def process(value: Row): Unit = {
              println(value)
            }

            override def close(errorOrNull: Throwable): Unit = closed = true
          })
          val streamingQuery: StreamingQuery = query.start()
          while (streamingQuery.lastProgress == null) {
            Thread.sleep(100)
          }
          if (closed) {
            streamingQuery.stop()
          }
        case _ => ""
      }
      NotImplementedYet
      // ----- End of unmanaged code area for action  Storage_managerApiYaml.getdataset
    }

  }

}
