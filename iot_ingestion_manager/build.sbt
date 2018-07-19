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

organization := "it.gov.daf"

name         := "daf-iot-ingestion-manager"

scalaVersion := "2.11.12"

// Environment

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-target:jvm-1.8",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Xfuture"
)

enablePlugins(
  PlayScala,
  AutomateHeaderPlugin,
  SbtAvrohugger
)

// Dependencies

resolvers           ++= Resolvers.all

libraryDependencies ++= Dependencies.compile.all
libraryDependencies ++= Dependencies.test.all

// Licenses

licenses       += Licenses.apache2

headerLicense  := Licenses.header
headerMappings += (HeaderFileType.conf -> HeaderCommentStyle.HashLineComment)

// Publish

publishTo   := Repositories.publish.value

credentials += Repositories.credential

// Misc

parallelExecution in Test := false

fork in Test := true

autoAPIMappings := true

dependencyOverrides += "com.google.guava" % "guava" % "16.0.1" % "compile"


//name := "iotIngestionManager"
//
//version := "2.0.0"
//
//organization := "it.teamdigitale"
//
//scalaVersion := "2.11.12"
//
//resolvers += "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
//
//val log4j = "2.9.1"
//val kuduVersion = "1.4.0-cdh5.12.0"
//val kafkaVersion = "0.10.0-kafka-2.1.0"
//val sparkVersion = "2.2.0.cloudera1"
//val avroVersion = "1.7.5"
//val twitterBijectionVersion = "0.9.6"
//val scalatestVersion = "3.0.5"
//val hadoopVersion = "2.6.0-cdh5.12.0"
//val betterFilesVersion = "3.4.0"
//
//val avroLibs = Seq (
//  "org.apache.avro" % "avro" % avroVersion,
//  "com.twitter" %% "bijection-avro" % twitterBijectionVersion
//)
//
//val logLibraries = Seq (
//  "org.apache.logging.log4j" % "log4j-core" % log4j,
//  "org.apache.logging.log4j" % "log4j-api" % log4j,
//  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j
//)
//
//val kudu = Seq (
//  "org.apache.kudu" % "kudu-client" % kuduVersion % "compile" ,
//  "org.apache.kudu" %% "kudu-spark2" % kuduVersion % "compile"
//)
//
//val spark = Seq (
//  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
//  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
//  "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided",
//  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion % "compile"
//)
//
//val hadoopTest = Seq (
//  "org.apache.hadoop" % "hadoop-minicluster" % hadoopVersion % "test",
//  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" classifier "tests",
//  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" classifier "tests" extra "type" -> "test-jar",
//  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" extra "type" -> "test-jar",
//  "org.apache.hadoop" % "hadoop-client" % hadoopVersion % "test" classifier "tests",
//  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" classifier "tests",
//  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" classifier "tests" extra "type" -> "test-jar"
//)
//
//val kuduTest = Seq (
//  "org.apache.kudu" % "kudu-client" % kuduVersion % "test" classifier "tests",
//  "org.apache.kudu" % "kudu-client" % kuduVersion % "test" classifier "tests" extra "type" -> "test-jar"
//)
//
//val kafkaTest = Seq (
//  "org.apache.kafka" %% "kafka" % kafkaVersion % "test" classifier "test",
//  //"org.apache.kafka" % "kafka-clients" % kafkaVersion % "compile",
//  "org.apache.kafka" % "kafka-clients" % kafkaVersion % "test" classifier "test"
//)
//
//libraryDependencies ++= Seq(
//  "com.typesafe" % "config" % "1.3.1",
//  "org.scalatest" %% "scalatest" % scalatestVersion % "test",
//  "org.scalactic" %% "scalactic" % scalatestVersion % "test",
//  "com.github.pathikrit" %% "better-files" % betterFilesVersion % "test",
//  "com.cloudera.livy" % "livy-client-http" % "0.3.0"
//) ++ logLibraries ++ kudu ++ spark ++ avroLibs ++ hadoopTest ++ kuduTest



//avroSpecificScalaSource in Compile := new java.io.File(s"${baseDirectory.value}/src/generated/scala")

avroSpecificSourceDirectory in Compile := baseDirectory.value / "avro"

sourceGenerators in Compile += (avroScalaGenerateSpecific in Compile).taskValue