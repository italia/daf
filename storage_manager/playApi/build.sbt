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
import com.typesafe.sbt.packager.docker.Cmd
import sbt.Keys.{resolvers, scalaVersion}
//import uk.gov.hmrc.gitstamp.GitStampPlugin._
import sbt.Resolver

//Seq(gitStampSettings: _*)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Xfuture"
)

val commonVersion = sys.env.getOrElse("COMMON_VERSION", "1.0-alpha.1")

lazy val core = RootProject(file("../core"))

lazy val root = (project in file("."))
  .settings(
    organization := "it.gov.daf",
    name := "daf-storage-manager",
    scalaVersion := "2.11.12",
    version in ThisBuild := sys.env.getOrElse("STORAGE_MANAGER_VERSION", "1.0-alpha.1"),
    autoAPIMappings := true,
    libraryDependencies ++= Seq(
      "com.typesafe.play"      %% "play-cache"           % playVersion,
      "com.typesafe.play"      %% "play-ws"              % playVersion,
      "com.typesafe.play"      %% "play-json"            % playVersion,
      "org.webjars"             % "swagger-ui"           % swaggerUiVersion,
      "io.swagger"             %% "swagger-play2"        % "1.5.3",
      "io.prometheus"           % "simpleclient"         % "0.1.0",
      "io.prometheus"           % "simpleclient_hotspot" % "0.1.0",
      "io.prometheus"           % "simpleclient_common"  % "0.1.0",
      "org.scalaj"             %% "scalaj-http"          % "2.3.0",
      "ch.qos.logback"          % "logback-classic"      % "1.2.3"            % Test,
      "com.typesafe.play"      %% "play-specs2"          % playVersion        % Test,
      "org.scalatest"          %% "scalatest"            % "3.0.4"            % Test,
      "org.scalatestplus.play" %% "scalatestplus-play"   % "2.0.1"            % Test,
      "com.github.pathikrit"   %% "better-files"         % betterFilesVersion % Test
    ).map(_.exclude("org.slf4j", "*")) ++ hbaseLibrariesTEST.map(_.exclude("org.slf4j", "*")),

    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.sonatypeRepo("releases"),
      "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
      "daf repo" at "http://nexus.daf.teamdigitale.it:8081/repository/maven-public/"
    )
  )
  .enablePlugins(PlayScala, AutomateHeaderPlugin, DockerPlugin)
  .aggregate(core)
  .dependsOn(core % "compile->compile;test->test")

val hadoopHBaseExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("javax.servlet", "servlet-api").
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl").
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.jboss.netty", "netty").
//    exclude("io.netty", "netty").
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
//    exclude("io.netty", "netty").
    exclude("commons-logging", "commons-logging").
    exclude("org.apache.xmlgraphics", "batik-ext").
    exclude("commons-collections", "commons-collections").
    exclude("xom", "xom").
    exclude("commons-beanutils", "commons-beanutils").
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl").
    exclude("org.slf4j", "slf4j-log4j12")

val hbaseLibrariesTEST = Seq(
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

javaOptions in Universal ++= Seq("-jvm-debug 5005")

licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
headerLicense := Some(HeaderLicense.ALv2("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"))
headerMappings := headerMappings.value + (HeaderFileType.conf -> HeaderCommentStyle.HashLineComment)

dockerBaseImage := "anapsix/alpine-java:8_jdk_unlimited"
dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("FROM", _) => List(cmd,
    Cmd("RUN", "apk update && apk add bash krb5-libs krb5"),
    Cmd("RUN", "ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf")
  )
  case other => List(other)
}
dockerEntrypoint := Seq(s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
dockerExposedPorts := Seq(9000)
dockerRepository := Option("nexus.daf.teamdigitale.it")

publishTo := {
  val nexus = "http://nexus.daf.teamdigitale.it:8081/repository/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases" at nexus + "maven-releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

parallelExecution in Test := false
