name := "core"

version := "0.1.0"

organization := "it.teamdigitale"

scalaVersion := "2.11.12"

val log4j = "2.9.1"
val kuduVersion = "1.4.0-cdh5.12.0"
val kafkaVersion = "0.10.0-kafka-2.1.0"
val sparkVersion = "2.2.0.cloudera1"
val avroVersion = "1.7.5"
val twitterBijectionVersion = "0.9.6"
val scalatestVersion = "3.0.5"
val hadoopVersion = "2.6.0-cdh5.12.0"
val betterFilesVersion = "3.4.0"

val avroLibs = Seq (
  "org.apache.avro" % "avro" % avroVersion,
  "com.twitter" %% "bijection-avro" % twitterBijectionVersion
)

val logLibraries = Seq (
  "org.apache.logging.log4j" % "log4j-core" % log4j,
  "org.apache.logging.log4j" % "log4j-api" % log4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j
)

val kudu = Seq (
  "org.apache.kudu" % "kudu-client" % kuduVersion % "compile" ,
  "org.apache.kudu" %% "kudu-spark2" % kuduVersion % "compile"
)

val spark = Seq (
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion % "provided"
)

val hadoopTest = Seq (
  "org.apache.hadoop" % "hadoop-minicluster" % hadoopVersion % "test",
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" classifier "tests",
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" classifier "tests" extra "type" -> "test-jar",
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" extra "type" -> "test-jar",
  "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "test" classifier "tests",
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" classifier "tests",
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" classifier "tests" extra "type" -> "test-jar"
)

val kuduTest = Seq (
  "org.apache.kudu" % "kudu-client" % kuduVersion % "test" classifier "tests",
  "org.apache.kudu" % "kudu-client" % kuduVersion % "test" classifier "tests" extra "type" -> "test-jar"
)

val kafkaTest = Seq (
  "org.apache.kafka" %% "kafka" % kafkaVersion % "test" classifier "test",
  //"org.apache.kafka" % "kafka-clients" % kafkaVersion % "compile",
  "org.apache.kafka" % "kafka-clients" % kafkaVersion % "test" classifier "test"
)

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "org.scalatest" %% "scalatest" % scalatestVersion % "test",
  "org.scalactic" %% "scalactic" % scalatestVersion % "test",
  "com.github.pathikrit" %% "better-files" % betterFilesVersion % "test",
  "com.cloudera.livy" % "livy-client-http" % "0.3.0"
) ++ logLibraries ++ kudu ++ spark ++ avroLibs ++ hadoopTest ++ kuduTest

avroSpecificScalaSource in Compile := new java.io.File(s"${baseDirectory.value}/src/generated/scala")
sourceGenerators in Compile += (avroScalaGenerateSpecific in Compile).taskValue

enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)

//universal plugin setting
//name in Universal := name.value


// docker building info
//dockerBaseImage := "anapsix/alpine-java:8_jdk_unlimited"
//
//dockerCommands := dockerCommands.value.flatMap {
//  case cmd@Cmd("FROM", _) => List(cmd,
//    Cmd("RUN", "apk update && apk add bash krb5-libs krb5"),
//    Cmd("RUN", "ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf")
//  )
//  case other => List(other)
//}
//
//dockerEntrypoint := Seq(s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
//
//dockerRepository := Option("10.98.74.120:5000")
//
//publishTo in ThisBuild := {
//  val nexus = "http://nexus.default.svc.cluster.local:8081/repository/"
//  if (isSnapshot.value)
//    Some("snapshots" at nexus + "maven-snapshots/")
//  else
//    Some("releases" at nexus + "maven-releases/")
//}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
