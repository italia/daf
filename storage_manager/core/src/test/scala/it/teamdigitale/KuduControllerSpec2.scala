//package it.teamdigitale
//
//import org.apache.kudu.client.CreateTableOptions
//import org.apache.kudu.spark.kudu.KuduContext
//import org.apache.spark.sql.{DataFrame, SparkSession}
//import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
//import org.slf4j.{Logger, LoggerFactory}
//import services.DockerContainer
//import scala.collection.convert.decorateAsJava._
//import scala.collection.convert.decorateAsScala._
//
///**
//  * Since MiniKuduCluster requires a compiled 'kudu' binary. For this reason we use to test Kudu docker-java library.
//  */
//class KuduControllerSpec2  extends FlatSpec with Matchers with BeforeAndAfterAll {
//
//  //var  miniCluster: MiniKuduCluster = _
//
//  //var client: AsyncKuduClient = _
//
//  //var syncClient: KuduClient = _
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
//  var containerId: String = _
//
//
//
//  override def beforeAll(): Unit = {
//
//    val image = "kunickiaj/kudu:1.0.1"
//    alogger.info(s"Pulling the image ${}")
//
//    containerId = DockerContainer.builder()
//      //.withImage("docker.elastic.co/elasticsearch/elasticsearch:5.5.2")
//      .withImage("kunickiaj/kudu:1.0.1")
//      //for test use unconvetional ports
//      .withPort(s"8050/tcp", "8050/tcp")
//      .withPort(s"8051/tcp", "8051/tcp")
//      //.withEnv("http.host", "0.0.0.0")
//      .withEnv("network.host", "0.0.0.0")
//      .withEnv("transport.host", "127.0.0.1")
//      //.withEnv("index.mapper.dynamic", "true")
//      .withName("TESTKUDU-MASTER")
//      .run()
//
////    miniCluster = Try (new MiniKuduCluster.MiniKuduClusterBuilder().numMasters(1).numTservers(1).build()) match {
////      case Success(c) => c
////      case Failure(ex) =>
////        alogger.error("NUOOOOOOOOOOOO")
////        new MiniKuduCluster.MiniKuduClusterBuilder().numMasters(1).numTservers(1).build()
////    }
////    alogger.info("Kudu MiniCluster created")
////
////    val masterAddresses: String = miniCluster.getMasterAddresses
////    val masterHostPort= miniCluster.getMasterHostPorts.asScala.head
////    alogger.info(s"Kudu MiniCluster created at $masterAddresses $masterHostPort")
////
////    //client = new AsyncKuduClient.AsyncKuduClientBuilder(masterAddresses).defaultAdminOperationTimeoutMs(50000).build
////    //syncClient = new KuduClient(client)
//
//
//    val masterHostPort = "localhost"
//
//
//    Thread.sleep(40000L)
//
//    val kuduContext = new KuduContext(s"localhost:8051", sparkSession.sqlContext.sparkContext)
//    initKuduTable(kuduContext)
////
//    kuduController = new KuduController(sparkSession, s"localhost:8051")
//
//    alogger.info("End Before All")
//  }
//
//  override def afterAll(): Unit = {
////    DockerContainer.builder().withId(containerId).clean()
//    //miniCluster.shutdown()
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
//    val build = new CreateTableOptions().setNumReplicas(1).addHashPartitions(List("word").asJava, 3)
//    //client.syncClient.createTable(tableName, df.schema, build)
//    //create table
//    kuduContext.createTable(tableName, df.schema, Seq("word"), build)
//    // Insert data
//    kuduContext.insertRows(df, tableName)
//  }
//
//
//
//  "KuduController" should "Correctly read data from kudu" in {
//
//      alogger.info("In Body")
//// see here https://github.com/cloudera/kudu/blob/master/java/kudu-client/src/test/java/org/apache/kudu/client/BaseKuduTest.java
//      val newDf = kuduController.readData(tableName)
//      newDf.isSuccess === true
//      newDf.get.count === 4
//      newDf.get.columns.toSet === df.columns.toSet
//      newDf.get.collect().toSet === df.collect().toSet
//
//      alogger.info(s"container $containerId")
//      //ok
//    }
//
//
//}
