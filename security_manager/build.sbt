import Versions._
import CommonBuild._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := "daf_security_manager"

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

wartremoverErrors ++= Warts.allBut(Wart.Nothing, Wart.PublicInference, Wart.Any, Wart.Equals)
wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value / "target" / "scala-2.11" / "routes").toSeq
wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value / "target" / "scala-2.11" / "routes").toSeq
wartremoverExcluded ++= routes.in(Compile).value

lazy val root = (project in file(".")).enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser, AutomateHeaderPlugin, DockerPlugin)

scalaVersion := "2.11.8"

val hadoopExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-log4j12").
    exclude("org.slf4j", "slf4j-api")

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
  "it.gov.daf" %% "common" % version.value,
  specs2 % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.4" % Test,
  "org.specs2" %% "specs2-scalacheck" % "3.6" % Test,
  "me.jeffmay" %% "play-json-tests" % "1.3.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
) ++ hadoopLibraries

resolvers ++= Seq(
  "zalando-bintray" at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns),
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

apiFirstParsers := Seq(ApiFirstSwaggerParser.swaggerSpec2Ast.value).flatten

playScalaAutogenerateTests := false

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
daemonUser := "daf"
dockerCommands += ExecCmd("ENTRYPOINT", s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
dockerExposedPorts := Seq(9000)
dockerRepository := Option("10.103.136.239:5000")