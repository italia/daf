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
import sbt.Keys.resolvers
import uk.gov.hmrc.gitstamp.GitStampPlugin._

organization := "it.gov.daf"

name := "common"

Seq(gitStampSettings: _*)

version in ThisBuild := sys.env.get("COMMON_VERSION").getOrElse("1.0.4-SNAPSHOT")

//version := "1.0.1-SNAPSHOT"

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

wartremoverErrors ++= Warts.allBut(Wart.Equals)

lazy val root = (project in file(".")).enablePlugins(AutomateHeaderPlugin)

scalaVersion := "2.11.8"

val hadoopExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.slf4j", "slf4j-api").
    exclude("javax.servlet", "servlet-api")

val hadoopLibraries = Seq(
  hadoopExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % Compile)
)

val playLibraries = Seq(
  "io.swagger" %% "swagger-play2" % "1.5.3",
  "com.typesafe.play" %% "play-cache" % playVersion,
  "com.typesafe.play" %% "filters-helpers" % playVersion,
  "com.typesafe.play" %% "play-ws" % playVersion,
  "com.github.cb372" %% "scalacache-guava" % "0.9.4",
  "org.pac4j" % "play-pac4j" % playPac4jVersion,
  "org.pac4j" % "pac4j-http" % pac4jVersion,
  "org.pac4j" % "pac4j-jwt" % pac4jVersion exclude("commons-io", "commons-io"),
  "org.pac4j" % "pac4j-ldap" % pac4jVersion
)

libraryDependencies ++= hadoopLibraries ++ playLibraries

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)

licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
headerLicense := Some(HeaderLicense.ALv2("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"))
headerMappings := headerMappings.value + (HeaderFileType.conf -> HeaderCommentStyle.HashLineComment)

publishTo := {
  val nexus = "http://nexus.daf.teamdigitale.it/repository/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases" at nexus + "maven-releases/")
}

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
