import sbt.{ExclusionRule, ModuleID, Resolver}

val sparkVersion = "2.2.0.cloudera1"
val hadoopVersion = "2.6.0-cdh5.12.0"
val hbaseVersion = "1.2.0-cdh5.12.0"
val log4j = "2.9.1"
val sparkOpenTSDBVersion = "2.0"
val scalaTestVersion = "3.0.4"
val betterFilesVersion = "2.17.1"
val spec2Version = "3.9.5"
val kuduVersion = "1.4.0-cdh5.12.0"

name := "daf-storage-manager-common"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.12"

fork in Test := true
javaOptions ++= Seq("-Xms4000M", "-Xmx4000M", "-XX:+CMSClassUnloadingEnabled")

val sparkExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.apache.hadoop", "hadoop-client").
    exclude("org.apache.hadoop", "hadoop-yarn-client").
    exclude("org.apache.hadoop", "hadoop-yarn-api").
    exclude("org.apache.hadoop", "hadoop-yarn-common").
    exclude("org.apache.hadoop", "hadoop-yarn-server-common").
    exclude("org.apache.hadoop", "hadoop-yarn-server-web-proxy").
    exclude("org.apache.zookeeper", "zookeeper").
    exclude("commons-collections", "commons-collections").
    exclude("commons-beanutils", "commons-beanutils").
    exclude("org.slf4j", "slf4j-log4j12")

val sparkLibraries = Seq(
  sparkExcludes("org.apache.spark" %% "spark-core" % sparkVersion % "compile"),
  sparkExcludes("org.apache.spark" %% "spark-sql" % sparkVersion % "compile"),
  //sparkExcludes("org.apache.spark" %% "spark-yarn" % sparkVersion % "compile"),
  //sparkExcludes("org.apache.spark" %% "spark-mllib" % sparkVersion % "compile"),
  sparkExcludes("org.apache.spark" %% "spark-streaming" % sparkVersion % "compile"),
  "com.databricks" %% "spark-avro" % "4.0.0" % "compile"
  //sparkExcludes("org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion % "compile"),
)

val hadoopExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-api").
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.mortbay.jetty", "jetty").
    exclude("org.mortbay.jetty", "jetty-util").
    exclude("org.mortbay.jetty", "jetty-sslengine").
    exclude("javax.servlet", "servlet-api").
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl").
    exclude("org.slf4j", "slf4j-log4j12")

val hadoopHBaseExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("javax.servlet", "servlet-api").
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl").
    exclude("org.slf4j", "slf4j-log4j12").
    excludeAll(ExclusionRule(organization = "javax.servlet"))

val hbaseExcludes =
  (moduleID: ModuleID) => moduleID.
    exclude("org.apache.thrift", "thrift").
    exclude("org.jruby", "jruby-complete").
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.mortbay.jetty", "jsp-2.1").
    exclude("org.mortbay.jetty", "jsp-api-2.1").
    exclude("org.mortbay.jetty", "servlet-api-2.5").
    exclude("com.sun.jersey", "jersey-core").
    exclude("com.sun.jersey", "jersey-json").
    exclude("com.sun.jersey", "jersey-server").
    exclude("org.mortbay.jetty", "jetty").
    exclude("org.mortbay.jetty", "jetty-util").
    exclude("tomcat", "jasper-runtime").
    exclude("tomcat", "jasper-compiler").
    exclude("org.jboss.netty", "netty").
    exclude("io.netty", "netty").
    exclude("commons-logging", "commons-logging").
    exclude("org.apache.xmlgraphics", "batik-ext").
    exclude("commons-collections", "commons-collections").
    exclude("xom", "xom").
    exclude("commons-beanutils", "commons-beanutils").
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl").
    exclude("org.slf4j", "slf4j-log4j12")

val logLibraries = Seq (
  "org.apache.logging.log4j" % "log4j-core" % log4j,
  "org.apache.logging.log4j" % "log4j-api" % log4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j
)

val hbaseLibraries = Seq (
  "org.apache.spark.opentsdb" %% "spark-opentsdb" % sparkOpenTSDBVersion % "compile" exclude("org.slf4j", "slf4j-log4j12"),
  hbaseExcludes("org.apache.hbase" % "hbase-client" % hbaseVersion % "compile"),
  hbaseExcludes("org.apache.hbase" % "hbase-protocol" % hbaseVersion % "compile"),
  hbaseExcludes("org.apache.hbase" % "hbase-hadoop-compat" % hbaseVersion % "compile"),
  hbaseExcludes("org.apache.hbase" % "hbase-server" % hbaseVersion % "compile"),
  hbaseExcludes("org.apache.hbase" % "hbase-common" % hbaseVersion % "compile"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-yarn-server-web-proxy" % hadoopVersion % "compile"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % "compile"),
  hadoopHBaseExcludes("org.apache.hbase" % "hbase-server" % hbaseVersion % "test" classifier "tests"),
  hadoopHBaseExcludes("org.apache.hbase" % "hbase-common" % hbaseVersion % "test" classifier "tests"),
  hadoopHBaseExcludes("org.apache.hbase" % "hbase-testing-util" % hbaseVersion % "test" classifier "tests"
    exclude("org.apache.hadoop<", "hadoop-hdfs")
    exclude("org.apache.hadoop", "hadoop-minicluster")
    exclude("org.apache.hadoo", "hadoop-client")),
  hadoopHBaseExcludes("org.apache.hbase" % "hbase-hadoop-compat" % hbaseVersion % "test" classifier "tests"),
  hadoopHBaseExcludes("org.apache.hbase" % "hbase-hadoop-compat" % hbaseVersion % "test" classifier "tests" extra "type" -> "test-jar"),
  hadoopHBaseExcludes("org.apache.hbase" % "hbase-hadoop2-compat" % hbaseVersion % "test" classifier "tests" extra "type" -> "test-jar"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % "test" classifier "tests"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" classifier "tests"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" classifier "tests" extra "type" -> "test-jar"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" extra "type" -> "test-jar"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % "test" classifier "tests"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-minicluster" % hadoopVersion % "test"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" classifier "tests" extra "type" -> "test-jar"),
  hadoopHBaseExcludes("org.apache.hadoop" % "hadoop-mapreduce-client-jobclient" % hadoopVersion % "test" classifier "tests")
)

val kuduLibraries = Seq(
  "org.apache.kudu" % "kudu-client" % kuduVersion % "compile",
  //to remove
  "org.apache.kudu" % "kudu-client" % kuduVersion % "test" classifier "tests" ,
  "org.apache.kudu" % "kudu-client" % kuduVersion % "test" classifier "tests" extra "type" -> "test-jar",
  //
  "com.github.docker-java" % "docker-java" % "3.0.13" % "test",
  "org.apache.kudu" %% "kudu-spark2" % kuduVersion % "compile"
)

//dependencyOverrides ++= Seq(
//  //"com.google.guava" % "guava" % "16.0.1" % "test",
//  //usefull for docker-java
//  "org.apache.httpcomponents" % "httpcore" % "4.4.5" % "compile",
//  "com.google.guava" % "guava" % "16.0.1" % "compile"
//)

dependencyOverrides += "com.google.guava" % "guava" % "16.0.1" % "compile"

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("releases"),
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "daf repo" at "http://nexus.default.svc.cluster.local:8081/repository/maven-public/"
)


libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalactic" %% "scalactic" % "3.0.4" % "test",
  "com.github.pathikrit" %% "better-files" % betterFilesVersion % Test)
  .map(x => x.exclude("org.scalactic", "scalactic"))
  .map(x => x.exclude("org.slf4j", "*")) ++
  sparkLibraries.map(x => x.exclude("org.slf4j", "*")) ++
  hbaseLibraries.map(x => x.exclude("org.slf4j", "*")) ++
  kuduLibraries.map(x => x.exclude("org.slf4j", "*"))
  //++ logLibraries.map(x => x.exclude("org.slf4j", "*"))

