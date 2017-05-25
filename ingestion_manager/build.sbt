import CommonBuild._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.zalando.play.generator.sbt.ApiFirstPlayScalaCodeGenerator.autoImport.playScalaAutogenerateTests
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys.resolvers

name := "daf-ingestion-manager"

version := "1.0.0"

lazy val sparkVersion = "2.0.0"
lazy val spark = "org.apache.spark"

lazy val root = (project in file(".")).enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.webjars" % "swagger-ui" % "3.0.7",
  specs2 % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.specs2" %% "specs2-scalacheck" % "3.8.9" % Test,
  "me.jeffmay" %% "play-json-tests" % "1.5.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "it.teamdigitale" %% "ingestion-module" % "0.1.0" exclude("org.apache.avro", "avro")
)




resolvers ++= Seq(
  "zalando-bintray" at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns),
  Resolver.mavenLocal
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

apiFirstParsers := Seq(ApiFirstSwaggerParser.swaggerSpec2Ast.value).flatten

playScalaAutogenerateTests := true

headers := Map(
  "sbt" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"),
  "scala" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"),
  "conf" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#"),
  "properties" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#"),
  "yaml" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#")
)

dockerBaseImage := "frolvlad/alpine-oraclejdk8:latest"
dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("FROM", _) => List(cmd, Cmd("RUN", "apk update && apk add bash"))
  case other => List(other)
}
dockerCommands += ExecCmd("ENTRYPOINT", s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
dockerExposedPorts := Seq(9000)

// Wart Remover Plugin Configuration
wartremoverErrors ++= Warts.allBut(Wart.Nothing, Wart.PublicInference, Wart.Any, Wart.Equals)

wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value / "target" / "scala-2.11" / "routes").toSeq
