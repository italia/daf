import CommonBuild._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.zalando.play.generator.sbt.ApiFirstPlayScalaCodeGenerator.autoImport.playScalaAutogenerateTests
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys.resolvers

organization in ThisBuild := "it.almawave"

name := "semantic-repository"

version in ThisBuild := "0.1.0"

val playVersion = "2.5.14"

// default port
PlayKeys.playDefaultPort := 8888

lazy val root = (project in file(".")).enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser)

scalaVersion in ThisBuild := "2.11.8"

crossPaths := false

libraryDependencies ++= Seq(
	cache,
	ws,
	filters,
	"org.webjars" % "swagger-ui" % "3.0.7",
	specs2 % Test,
	"org.scalacheck" %% "scalacheck" % "1.12.4" % Test,
	"me.jeffmay" %% "play-json-tests" % "1.5.0" % Test,
	"org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % Test,
	"org.seleniumhq.selenium" % "selenium-java" % "2.48.2",
	"com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.1",

"org.eclipse.rdf4j" % "rdf4j-runtime" % "2.2.2",
"org.eclipse.rdf4j" %  "rdf4j-repository-sail" % "2.2.2",
"org.eclipse.rdf4j" % "rdf4j-repository-api" % "2.2.2",
"org.eclipse.rdf4j" % "rdf4j-sail-memory" % "2.2.2",
"org.eclipse.rdf4j" % "rdf4j-sail-nativerdf" % "2.2.2",
"com.github.jsonld-java" % "jsonld-java" % "0.9.0",

  
"org.scalatest" %% "scalatest" % "2.2.2" % Test,
"junit" % "junit" % "4.11" % Test,
"com.novocode" % "junit-interface" % "0.11" % Test,

// "org.eclipse.rdf4j" % "rdf4j-sail-solr" % "2.2.2", 
// "org.apache.solr" % "solr-solrj" % "5.1.0", 
// "org.apache.solr" % "solr-core" % "5.1.0", 
// TODO: jdk.tools
  
  // DISABLED: now copied source internally. CHECK sub-modules?
  // "it.awave.kb" % "kb-core" % "0.0.1", // CHECK: see how to point to maven local!
  // "it.almawave.linkeddata.kb" % "kb-core_2.11" % "0.0.1"  cross CrossVersion.Disabled ,
  // withSources() withJavadoc() cross CrossVersion.full 
  
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
 
)

//dependencyOverrides ++= Set(
//	"com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.1"
//)


resolvers ++= Seq(
  Resolver.mavenLocal,
  //"Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
  "zalando-bintray" at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns)
)

// resolver for local maven repository
// CHECK: maven local artifacts does not conforms to expected scala version specific name
// resolvers += "Local Maven Repository" at s"file://${Path.userHome.absolutePath}/.m2/repository"
resolvers += Resolver.mavenLocal // Also use $HOME/.m2/repository



// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

apiFirstParsers := Seq(ApiFirstSwaggerParser.swaggerSpec2Ast.value).flatten


// play.modules.enabled += "modules.OnStartupModule"

playScalaAutogenerateTests := false

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

dockerCommands += ExecCmd("ENTRYPOINT", s"bin/${name.value}", "-Dconfig.file=conf/production.conf")
dockerExposedPorts := Seq(9000)
dockerRepository := Option("10.98.74.120:5000")


// WART
// wartremoverErrors ++= Warts.unsafe
// Wart Remover Plugin Configuration
// wartremoverErrors ++= Warts.allBut(Wart.Nothing, Wart.PublicInference, Wart.Any, Wart.Equals)
// wartremoverExcluded ++= getRecursiveListOfFiles(baseDirectory.value / "target" / "scala-2.11" / "routes").toSeq

