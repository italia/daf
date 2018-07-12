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

import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{ Docker => DockerGoal }

organization := "it.gov.daf"

name         := "daf-storage-manager"

scalaVersion := "2.11.12"

// Environment

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

enablePlugins(
  PlayScala,
  AutomateHeaderPlugin,
  DockerPlugin
)

// Dependencies

resolvers           ++= Resolvers.all

libraryDependencies ++= Dependencies.compile.all
libraryDependencies ++= Dependencies.test.all

unmanagedBase       := file("libext")

// Licenses

licenses       += Licenses.apache2

headerLicense  := Licenses.header
headerMappings += (HeaderFileType.conf -> HeaderCommentStyle.HashLineComment)

// Docker

dockerBaseImage                      := Docker.base
dockerPackageMappings in DockerGoal ++= Docker.mappings
dockerCommands                       ~= Docker.appendSecurity
dockerCommands                       ~= Docker.updateEnvironment("DAF_STORAGE_MANAGER_ARTIFACT", s"it.gov.daf.daf-storage-manager-${Versions.defaultVersion}-sans-externalized.jar")
dockerEntrypoint                     := Docker.entryPoint { name.value }
dockerExposedPorts                   := Docker.ports
dockerRepository                     := Docker.repository

// Publish

publishTo   := Repositories.publish.value

credentials += Repositories.credential

// Misc

parallelExecution in Test := false

fork in Test := true

autoAPIMappings := true

dependencyOverrides += "com.google.guava" % "guava" % "16.0.1" % "compile"
