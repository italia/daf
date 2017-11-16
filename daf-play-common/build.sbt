import sbt.{Credentials, Path}

val playVersion = "2.5.14"
val playPac4jVersion = "3.0.0"
val pac4jVersion = "2.0.0"

val libraries = Seq(
  "com.github.cb372" %% "scalacache-caffeine" % "0.21.0",
  "com.typesafe.play" % "play_2.11" % playVersion,
  "org.pac4j" % "play-pac4j" % playPac4jVersion,
  "org.pac4j" % "pac4j-http" % pac4jVersion,
  "org.pac4j" % "pac4j-jwt" % pac4jVersion exclude("commons-io", "commons-io"),
  "org.pac4j" % "pac4j-ldap" % pac4jVersion,
  "com.typesafe.play" %% "filters-helpers" % playVersion,
  "com.typesafe.play" %% "play-ws" % playVersion

//  ,
//  "org.apache.hadoop" % "hadoop-client" % hadoopVersion % Compile excludeAll(
//    ExclusionRule("org.slf4j", "slf4j-log4j12"),
//    ExclusionRule("org.slf4j", "slf4j-api"),
//    ExclusionRule("javax.servlet", "servlet-api")
//  )
)

lazy val root = (project in file("."))
    .settings(
      organization := "it.gov.daf",
      name := "daf-play-common",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.12",
      libraryDependencies ++= libraries
    )

publishTo := {
  val nexus = "http://nexus.default.svc.cluster.local:8081/repository/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases" at nexus + "maven-releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

        