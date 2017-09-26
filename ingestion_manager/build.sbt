import CommonBuild._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.zalando.play.generator.sbt.ApiFirstPlayScalaCodeGenerator.autoImport.playScalaAutogenerateTests
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys.resolvers

organization in ThisBuild := "it.gov.daf"
name := "daf-ingestion-manager"
version := "1.0-SNAPSHOT"

val playVersion = "2.5.14"

lazy val client = (project in file("client")).
  settings(Seq(
    name := "daf-ingestion-manager-client",
    swaggerGenerateClient := true,
    swaggerClientCodeGenClass := new it.gov.daf.swaggergenerators.DafClientGenerator,
    swaggerCodeGenPackage := "it.gov.daf.ingestionmanager",
    swaggerSourcesDir := file(s"${baseDirectory.value}/../conf"),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playVersion,
      "com.typesafe.play" %% "play-ws" %  playVersion
    )
  )).enablePlugins(SwaggerCodegenPlugin)

lazy val root = (project in file(".")).enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser)
  .dependsOn(client).aggregate(client)

scalaVersion in ThisBuild := "2.11.8"



libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  "org.webjars" % "swagger-ui" % "3.0.10", //excludeAll( ExclusionRule(organization = "com.fasterxml.jackson.core") ),
//  "org.json4s" %% "json4s-jackson" % "3.5.2"  exclude("com.fasterxml.jackson.core", "jackson-databind"),
  specs2 % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "org.specs2" %% "specs2-scalacheck" % "3.8.9" % Test,
  //"me.jeffmay" %% "play-json-tests" % "1.5.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "me.lessis" %% "base64" % "0.2.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.9.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.9.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.9.0",
  "it.gov.daf" %% "common" % "1.0-SNAPSHOT",
  "it.gov.daf" %% "daf-catalog-manager-client" % "1.0.0-SNAPSHOT"
  )

playScalaCustomTemplateLocation := Some(baseDirectory.value / "templates")

resolvers ++= Seq(
  Resolver.mavenLocal,
  "zalando-bintray" at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maƒven",
  "daf repo" at "http://nexus.default.svc.cluster.local:8081/repository/maven-public/",
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
dockerRepository := Option("10.98.74.120:5000")


publishTo in ThisBuild := {
  val nexus = "http://nexus.default.svc.cluster.local:8081/repository/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases"  at nexus + "maven-releases/")
}
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

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
