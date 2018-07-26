/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package representation

import java.time.ZonedDateTime
import java.util.function.Supplier

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import it.gov.daf.catalogmanager.{ MetaCatalog, StorageInfo }

import scala.util.{ Failure, Success, Try }

final case class StreamData(id: String,
                            interval: Long,
                            owner: String,
                            source: Source,
                            sink: Sink,
                            schema: Map[String, String])

object StreamData {

  private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)

  private def source(catalog: MetaCatalog) = catalog.operational.input_src.srv_push.flatMap { _.headOption } match {
    case Some(service) => Success { KafkaSource(service.name) }
    case None          => Failure { new IllegalArgumentException(s"Unsupported input source for data @ [${catalog.operational.logical_uri}]: only [srv_push] is allowed") }
  }

  private def kuduSink(storageInfo: StorageInfo) = storageInfo.kudu.flatMap { _.table_name }.map { KuduSink }

  private def hdfsSink(storageInfo: StorageInfo) = storageInfo.hdfs.flatMap { _.path }.map { HdfsSink }

  private def sink(catalog: MetaCatalog) = catalog.operational.storage_info.flatMap { storage => kuduSink(storage) orElse hdfsSink(storage) } match {
    case Some(sink) => Success(sink)
    case None       => Failure { new IllegalArgumentException(s"No valid sink found in catalog for data @ [${catalog.operational.logical_uri}]") }
  }

  private def parseCronString(s: String) = Try {
    ExecutionTime.forCron { new CronParser(cronDefinition).parse(s) }
      .timeToNextExecution { ZonedDateTime.now() }
      .orElseThrow {
        new Supplier[Throwable] {
          def get() = new RuntimeException(s"Unable to determine execution interval from cron string [$s]")
        }
      }
      .getSeconds
  }

  private def interval(catalog: MetaCatalog) = catalog.operational.dataset_proc match {
    case Some(proc) if proc.dataset_type == "stream" => parseCronString(proc.cron)
    case Some(proc) => Failure { new IllegalArgumentException(s"Unable to generate stream data from catalog for data set of type [${proc.dataset_type}]") }
    case None       => Failure { new IllegalArgumentException(s"Missing [dataset_proc] attribute in catalog data @ [${catalog.operational.logical_uri}]") }
  }

  private def avroSchema(catalog: MetaCatalog) = catalog.dataschema.avro.fields match {
    case None         => Failure { new RuntimeException(s"Missing avro schema in catalog for data @ [${catalog.operational.logical_uri}]") }
    case Some(fields) => Try {
      fields.map { field => field.name -> field.`type` }.toMap[String, String]
    }
  }

  /**
    * Creates an instance instance by processing catalog data.
    * - `source` is extracted from the `operational.input_src.srv_push` path.
    * - `sinks` are extracted from the `operational.storage_info` path, with support for Kudu and HDFS
    * - `interval` is extracted by parsing the cron string `operational.dataset_proc.cron`
    * @param catalog the catalog data to process
    */
  def fromCatalog(catalog: MetaCatalog): Try[StreamData] = for {
    seconds <- interval(catalog)
    source  <- source(catalog)
    sink    <- sink(catalog)
    schema  <- avroSchema(catalog)
  } yield StreamData(
    id       = catalog.operational.logical_uri,
    interval = seconds,
    owner    = catalog.dcatapit.author getOrElse "<unknown>",
    source   = source,
    sink     = sink,
    schema   = schema
  )

}

sealed trait Source

case class KafkaSource(topic: String) extends Source

case class SocketSource(host: String, port: Int) extends Source


sealed trait Sink

case class KuduSink(tableName: String) extends Sink

case class HdfsSink(dir: String) extends Sink

case object ConsoleSink extends Sink