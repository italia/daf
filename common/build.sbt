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

val isStaging = System.getProperty("STAGING") != null

organization := "it.gov.daf"

name := "common"

Seq(gitStampSettings: _*)

version in ThisBuild := sys.env.getOrElse("COMMON_VERSION", "1.0-alpha.1")

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

wartremoverErrors ++= Warts.allBut(Wart.Equals, Wart.ImplicitParameter, Wart.Overloading)

lazy val root = (project in file(".")).enablePlugins(AutomateHeaderPlugin)

scalaVersion := "2.11.8"
/*
val hadoopExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.slf4j", "slf4j-api").
    exclude("javax.servlet", "servlet-api")

val hadoopLibraries = Seq(
  hadoopExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % Compile)
)
*/
val playLibraries = Seq(
  "io.swagger"        %% "swagger-play2"    % "1.5.3",
  "org.typelevel"     %% "cats-core"        % catsVersion,
  "org.typelevel"     %% "cats-effect"      % catsEffectVersion,
  "com.typesafe.play" %% "play-cache"       % playVersion,
  "com.typesafe.play" %% "filters-helpers"  % playVersion,
  "com.typesafe.play" %% "play-ws"          % playVersion,
  "com.github.cb372"  %% "scalacache-guava" % "0.9.4",
  "org.pac4j"          % "play-pac4j"       % playPac4jVersion,
  "org.pac4j"          % "pac4j-http"       % pac4jVersion,
  "org.pac4j"          % "pac4j-jwt"        % pac4jVersion exclude("commons-io", "commons-io"),
  "org.pac4j"          % "pac4j-ldap"       % pac4jVersion,
  "commons-codec"      % "commons-codec"    % "1.11"
)

//libraryDependencies ++= hadoopLibraries ++ playLibraries
libraryDependencies ++= playLibraries
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases")
//  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)

licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
headerLicense := Some(HeaderLicense.ALv2("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"))
headerMappings := headerMappings.value + (HeaderFileType.conf -> HeaderCommentStyle.HashLineComment)

publishTo := {
  val nexus = if(isStaging) "http://nexus.teamdigitale.test:8081/repository/"
              else "http://nexus.default.svc.cluster.local:8081/repository/"

  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases" at nexus + "maven-releases/")
}

publishMavenStyle := true

autoAPIMappings := true

credentials += {if(isStaging) Credentials(Path.userHome / ".ivy2" / ".credentialsTest") else Credentials(Path.userHome / ".ivy2" / ".credentials")}

