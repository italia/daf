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

import Versions._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt.Keys.resolvers

name := "daf_storage_manager"

version := "1.0.0"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Xfuture"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, AutomateHeaderPlugin, DockerPlugin)

scalaVersion := "2.11.8"

val hadoopExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.slf4j", "slf4j-api")

val sparkExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.slf4j", "slf4j-api").
    exclude("org.slf4j", "jcl-over-sl4j").
    exclude("org.slf4j", "jul-to-sl4j")

val sparkLibraries = Seq(
  sparkExcludes("org.apache.spark" %% "spark-core" % sparkVersion % Compile),
  sparkExcludes("org.apache.spark" %% "spark-sql" % sparkVersion % Compile),
  "com.databricks" %% "spark-avro" % "3.2.0" % Compile
)

val hadoopLibraries = Seq(
  hadoopExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % Compile),
  hadoopExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % Test classifier "tests"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % Test classifier "tests"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % Test classifier "tests" extra "type" -> "test-jar"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % Test extra "type" -> "test-jar"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % Test classifier "tests"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-minicluster" % hadoopVersion % Test),
  hadoopExcludes("org.apache.hadoop" % "hadoop-common" % hadoopVersion % Test classifier "tests" extra "type" -> "test-jar"),
  hadoopExcludes("org.apache.hadoop" % "hadoop-mapreduce-client-jobclient" % hadoopVersion % Test classifier "tests"),
  "com.github.pathikrit" %% "better-files" % betterFilesVersion % Test
)

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.webjars" % "swagger-ui" % swaggerUiVersion,
  specs2 % Test,
  "io.swagger" %% "swagger-play2" % "1.5.3",
  "com.typesafe.play" %% "play-json" % playVersion
) ++ hadoopLibraries ++ sparkLibraries

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)

headers := Map(
  "sbt" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"),
  "scala" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"),
  "conf" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#"),
  "properties" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#"),
  "yaml" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#")
)

dockerPackageMappings in Docker += (baseDirectory.value / "docker" / "krb5.conf") -> "krb5.conf"
dockerPackageMappings in Docker += (baseDirectory.value / "docker" / "principal.keytab") -> "principal.keytab"
dockerPackageMappings in Docker += (baseDirectory.value / "docker" / "start.sh") -> "start.sh"
dockerBaseImage := "anapsix/alpine-java:8_jdk_unlimited"
dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("FROM", _) => List(cmd,
    Cmd("RUN", "apk update && apk add bash krb5-libs krb5"),
    Cmd("ADD", "krb5.conf", "/etc/krb5.conf"),
    Cmd("ADD", "principal.keytab", "/opt/docker/conf/principal.keytab"),
    Cmd("ADD", "start.sh", "/opt/docker/bin/start.sh"),
    Cmd("RUN", "chmod +x /opt/docker/bin/start.sh")
  )
  case other => List(other)
}
//dockerCommands += ExecCmd("ENTRYPOINT", s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
dockerCommands += ExecCmd("ENTRYPOINT", s"bin/start.sh")
dockerExposedPorts := Seq(9000)
