import CommonBuild._
import Versions._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

organization in ThisBuild := "it.gov.daf"
name := "daf-security-manager"

version in ThisBuild := "1.0-SNAPSHOT"

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

wartremoverErrors ++= Warts.allBut(Wart.Nothing, Wart.PublicInference, Wart.Any, Wart.Equals, Wart.Option2Iterable)
wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value / "target" / "scala-2.11" / "routes").toSeq
wartremoverExcluded ++= routes.in(Compile).value

lazy val client = (project in file("client")).
  settings(Seq(
    name := "daf-security-manager-client",
    swaggerGenerateClient := true,
    swaggerClientCodeGenClass := new it.gov.daf.swaggergenerators.DafClientGenerator,
    swaggerCodeGenPackage := "it.gov.daf.securitymanager",
    swaggerModelFilesSplitting := "oneFilePerModel",
    swaggerSourcesDir := file(s"${baseDirectory.value}/../conf"),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playVersion,
      "com.typesafe.play" %% "play-ws" %  playVersion
    )
  )).
  enablePlugins(SwaggerCodegenPlugin)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser, /*AutomateHeaderPlugin,*/ DockerPlugin).
  dependsOn(client).aggregate(client)

scalaVersion in ThisBuild := "2.11.8"

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
  specs2 % Test
) ++ hadoopLibraries

resolvers ++= Seq(
  "zalando-bintray" at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns),
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "daf repo" at "http://nexus.default.svc.cluster.local:8081/repository/maven-public/"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

apiFirstParsers := Seq(ApiFirstSwaggerParser.swaggerSpec2Ast.value).flatten

playScalaAutogenerateTests := false

playScalaCustomTemplateLocation := Some(baseDirectory.value / "templates")

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
dockerCommands += ExecCmd("ENTRYPOINT", s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
dockerExposedPorts := Seq(9000)
dockerRepository := Option("10.98.74.120:5000")

publishTo in ThisBuild := {
  val nexus = "http://nexus.default.svc.cluster.local:8081/repository/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases"  at nexus + "maven-releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
