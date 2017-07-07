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

import java.io.{IOException, File => JFile}
import java.net.ServerSocket
import java.util.Base64

import it.gov.daf.iotingestionmanager.client.Iot_ingestion_managerClient
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseTestingUtility, TableName}
import org.apache.spark.SparkConf
import org.apache.spark.opentsdb.OpenTSDBConfigurator
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test.WithServer

import scala.collection.convert.decorateAsScala._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Var"
  )
)
class ServiceSpec extends Specification with BeforeAfterAll {

  def getAvailablePort: Int = {
    try {
      val socket = new ServerSocket(0)
      try {
        socket.getLocalPort
      } finally {
        socket.close()
      }
    } catch {
      case e: IOException =>
        throw new IllegalStateException(s"Cannot find available port: ${e.getMessage}", e)
    }
  }

  def application: Application = GuiceApplicationBuilder().
    //    configure("hadoop_conf_dir" -> s"${ServiceSpec.confPath.pathAsString}").
    configure("pac4j.authenticator" -> "test").
    build()

  "This test" should {
    "pass" in new WithServer(app = application, port = getAvailablePort) {
      val ws: AhcWSClient = AhcWSClient()

      val plainCreds = "david:david"
      val plainCredsBytes = plainCreds.getBytes
      val base64CredsBytes = Base64.getEncoder.encode(plainCredsBytes)
      val base64Creds = new String(base64CredsBytes)
      val client = new Iot_ingestion_managerClient(ws)(s"http://localhost:$port")
      val result = Await.result(client.start(s"Basic $base64Creds"), Duration.Inf)
    }
  }

  private val hbaseUtil = new HBaseTestingUtility()

  private var baseConf: Configuration = _

  override def beforeAll(): Unit = {
    hbaseUtil.startMiniCluster(4)
    val conf = new SparkConf().
      setAppName("spark-opentsdb-local-test").
      setMaster("local[4]").
      set("spark.io.compression.codec", "lzf")
    baseConf = hbaseUtil.getConfiguration
    hbaseUtil.createTable(TableName.valueOf("tsdb-uid"), Array("id", "name"))
    hbaseUtil.createTable(TableName.valueOf("tsdb"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-tree"), Array("t"))
    hbaseUtil.createTable(TableName.valueOf("tsdb-meta"), Array("name"))
    ()
  }

  override def afterAll(): Unit = {
    hbaseUtil.deleteTable("tsdb-uid")
    hbaseUtil.deleteTable("tsdb")
    hbaseUtil.deleteTable("tsdb-tree")
    hbaseUtil.deleteTable("tsdb-meta")
    hbaseUtil.shutdownMiniCluster()
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
object ServiceSpec {

}

class TestOpenTSDBConfigurator(mapConf: Map[String, String]) extends OpenTSDBConfigurator with Serializable {

  lazy val configuration: Configuration = mapConf.foldLeft(new Configuration(false)) { (conf, pair) =>
    conf.set(pair._1, pair._2)
    conf
  }

}

object TestOpenTSDBConfigurator {

  def apply(conf: Configuration): TestOpenTSDBConfigurator = new TestOpenTSDBConfigurator(
    conf.iterator().asScala.map { entry => entry.getKey -> entry.getValue }.toMap[String, String]
  )

}