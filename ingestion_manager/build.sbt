import CommonBuild._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.zalando.play.generator.sbt.ApiFirstPlayScalaCodeGenerator.autoImport.playScalaAutogenerateTests
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys.resolvers

organization in ThisBuild := "it.gov.daf"
name := "daf-ingestion-manager"
version := "1.0.0"

lazy val sparkVersion = "2.0.0"
lazy val spark = "org.apache.spark"

lazy val root = (project in file(".")).enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser)

scalaVersion in ThisBuild := "2.11.8"


def dependencyToProvide(scope: String = "compile") = Seq(
  spark %% "spark-core" % sparkVersion % scope exclude("com.fasterxml.jackson.core", "jackson-databind"),
  spark %% "spark-sql" % sparkVersion % scope exclude("com.fasterxml.jackson.core", "jackson-databind"),
  spark %% "spark-streaming" % sparkVersion % scope exclude("com.fasterxml.jackson.core", "jackson-databind")
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.webjars" % "swagger-ui" % "3.0.10", //excludeAll( ExclusionRule(organization = "com.fasterxml.jackson.core") ),
  "it.gov.daf" %% "daf-catalog-manager-client" % "1.0.0",
  "org.json4s" %% "json4s-jackson" % "3.5.2"  exclude("com.fasterxml.jackson.core", "jackson-databind"),
  "com.databricks" %% "spark-avro" % "3.2.0",
  specs2 % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.specs2" %% "specs2-scalacheck" % "3.8.9" % Test,
  //"me.jeffmay" %% "play-json-tests" % "1.5.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test) ++ dependencyToProvide()




resolvers ++= Seq(
  "zalando-bintray" at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns)//, Resolver.mavenLocal
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
wartremoverErrors ++= Warts.allBut(Wart.Nothing,
  Wart.PublicInference,
  Wart.Any,
  Wart.Equals,
  Wart.AsInstanceOf,
  Wart.DefaultArguments,
  Wart.OptionPartial)

//wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value / "target" / "scala-2.11" / "routes").toSeq
wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value).toSeq