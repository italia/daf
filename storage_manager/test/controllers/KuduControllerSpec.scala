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

//package it.teamdigitale
//
//import java.util
//
//import org.apache.kudu.{ColumnSchema, Schema, Type}
//import org.apache.kudu.client._
//import org.apache.kudu.spark.kudu.KuduContext
//import org.apache.spark.sql.{DataFrame, SparkSession}
//import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
//import org.slf4j.{Logger, LoggerFactory}
//
//import scala.collection.convert.decorateAsJava._
//import scala.collection.convert.decorateAsScala._
//import scala.util.{Failure, Success, Try}
//
//class KuduControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
//
//  var  miniCluster: MiniKuduCluster = _
//
//  var client: AsyncKuduClient = _
//
//  var syncClient: KuduClient = _
//
//  val tableName = "test_table"
//
//  var df: DataFrame = _
//
//  val sparkSession = SparkSession.builder().master("local").getOrCreate()
//
//  var kuduController: KuduController = _
//
//  val alogger: Logger = LoggerFactory.getLogger(this.getClass)
//
//
//
//  override def beforeAll(): Unit = {
//
//    alogger.info(s"${System.getProperty("user.dir")}/test/kudu_executables/${sun.awt.OSInfo.getOSType().toString.toLowerCase}")
//
//    System.setProperty(
//      "binDir",
//      s"${System.getProperty("user.dir")}/src/test/kudu_executables/${sun.awt.OSInfo.getOSType().toString.toLowerCase}"
//    )
//
////    miniCluster = Try (new MiniKuduCluster.MiniKuduClusterBuilder().build()) match {
////      case Success(c) => c
////      case Failure(ex) =>
////        alogger.error("NUOOOOOOOOOOOO")
////        new MiniKuduCluster.MiniKuduClusterBuilder().build()
////    }
//
//    //kudu master
//    miniCluster = new MiniKuduCluster.MiniKuduClusterBuilder().numTservers(3).defaultTimeoutMs(50000).build()
//    //Thread.sleep(50000)
//
//    val masterAddresses: String = miniCluster.getMasterAddresses
//    val masterHostPort= miniCluster.getMasterHostPorts.asScala.head
//    alogger.info(s"Kudu MiniCluster created at $masterAddresses $masterHostPort")
//
//    //client = new AsyncKuduClient.AsyncKuduClientBuilder(masterAddresses).defaultAdminOperationTimeoutMs(50000).build
//    //syncClient = new KuduClient(client)
//
//
//
//    val kuduClient = new KuduClient.KuduClientBuilder(masterAddresses).build
//    //val kuduContext = new KuduContext(s"$masterAddresses", sparkSession.sqlContext.sparkContext)
//    if (!miniCluster.waitForTabletServers(3)) {
//      alogger.error("Couldn't get " + 3 + " tablet servers running, aborting")
//    }
//
//    initKuduTable(kuduClient)
//    //initKuduTable(kuduContext)
//
//    kuduController = new KuduController(sparkSession, masterAddresses)
//
//    alogger.info("End Before All")
//  }
//
//  override def afterAll(): Unit = {
//
//    miniCluster.shutdown()
//  }
//
//  private def initKuduTable(kuduContext: KuduContext): Unit = {
//    import sparkSession.sqlContext.implicits._
//
//    df = Seq(
//      (8, "bat"),
//      (64, "mouse"),
//      (-27, "horse")
//    ).toDF("number", "word")
//
//    val build = new CreateTableOptions().setNumReplicas(1).addHashPartitions(List("word").asJava, 1)
//    //client.syncClient.createTable(tableName, df.schema, build)
//    //create table
//    kuduContext.createTable(tableName, df.schema, Seq("word"), build)
//    // Insert data
//    kuduContext.insertRows(df, tableName)
//  }
//
//
//  private def initKuduTable(kuduClient: KuduClient) : Unit = {
//    import sparkSession.sqlContext.implicits._
//
//    df = Seq(
//      (8, "bat"),
//      (64, "mouse"),
//      (-27, "horse")
//    ).toDF("number", "word")
//
//    val l =  Seq(
//      (8, "bat"),
//      (64, "mouse"),
//      (-27, "horse")
//    )
//
//    val build = new CreateTableOptions().setNumReplicas(1).addHashPartitions(List("word").asJava, 3)
//    val colNumber = new ColumnSchema.ColumnSchemaBuilder("number", Type.INT32).key(true).build()
//    val colWord = new ColumnSchema.ColumnSchemaBuilder("word", Type.STRING).build()
//    val cols = List(colNumber, colWord).asJava
//    val kuduSchema = new Schema(cols)
//
//
//    val table = client.syncClient.createTable(tableName, kuduSchema, build)
//
//    //val table = client.syncClient.openTable(tableName)
//    val session = client.syncClient.newSession
//
//    val resp = l.map{case (n, w) =>
//      val insert = table.newInsert()
//      val row = insert.getRow
//      row.addInt(0, n)
//      row.addString(1, w)
//      session.apply(insert);
//    }
//    resp.foreach(x => x.hasRowError)
//  }
//
//  "KuduController" should "Correctly read data from kudu" in {
//
//    alogger.info("In Body")
//    // see here https://github.com/cloudera/kudu/blob/master/java/kudu-client/src/test/java/org/apache/kudu/client/BaseKuduTest.java
//    val newDf = kuduController.readData(tableName)
//
//    newDf shouldBe 'Success
//    newDf.get.count === 4
//    newDf.get.columns.toSet === df.columns.toSet
//    newDf.get.collect().toSet === df.collect().toSet
//    newDf.foreach(_.show())
//  }
//
//}
