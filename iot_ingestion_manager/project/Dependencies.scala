import sbt._

object Dependencies {

  object compile {

    object internal {

      lazy val common  = "it.gov.daf" %% "common" % Versions.common

      lazy val catalog = "it.gov.daf" %% "daf-catalog-manager-client" % Versions.catalog

    }

    object play {

      val cache = "com.typesafe.play" %% "play-cache" % Versions.play

      val ws    = "com.typesafe.play" %% "play-ws"    % Versions.play

      val json  = "com.typesafe.play" %% "play-json"  % Versions.play

      val all = Seq(cache, ws, json)

    }

    object hadoop {

      val yarnServerProxy = "org.apache.hadoop" % "hadoop-yarn-server-web-proxy" % Versions.hadoop

      val client          = "org.apache.hadoop" % "hadoop-client"                % Versions.hadoop

      val all = Seq(yarnServerProxy, client).map { Exclusions.hadoop _ andThen Exclusions.jetty }

    }

    object spark {

      val core      = "org.apache.spark" %% "spark-core"           % Versions.spark

      val sql       = "org.apache.spark" %% "spark-sql"            % Versions.spark

      val streaming = "org.apache.spark" %% "spark-streaming"      % Versions.spark

      val kafka     = "org.apache.spark" %% "spark-sql-kafka-0-10" % Versions.spark

      val avro      = "com.databricks"   %% "spark-avro"           % Versions.sparkAvro

      val all = Seq(core, sql, streaming, kafka, avro).map {  Exclusions.spark }

    }

    object kafka {

      val core    = "org.apache.kafka" %% "kafka"         % Versions.kafka

      val clients = "org.apache.kafka"  % "kafka-clients" % Versions.kafka

      val tools   = "org.apache.kafka"  % "kafka-tools"   % Versions.kafka

      val all = Seq(core, clients, tools)

    }

    object kudu {

      val client = "org.apache.kudu"  % "kudu-client" % Versions.kudu

      val spark2 = "org.apache.kudu" %% "kudu-spark2" % Versions.kudu

      val all = Seq(client, spark2)

    }

    object avro {

      val bijection  = "com.twitter"          %% "bijection-avro" % Versions.avroBijection

      val avro4sCore =  "com.sksamuel.avro4s" %% "avro4s-core"    % Versions.avro4s

      val avro4sJson =  "com.sksamuel.avro4s" %% "avro4s-json"    % Versions.avro4s

      val all = Seq(bijection, avro4sCore, avro4sJson)

    }

    val typesafeConfig = "com.typesafe"     % "config"        % Versions.typesafeConfig

    val shapeless      = "com.chuusai"     %% "shapeless"     % Versions.shapeless

    val catsCore       = "org.typelevel"   %% "cats-core"     % Versions.catsCore

    val catsFree       = "org.typelevel"   %% "cats-free"     % Versions.catsCore

    val guava          = "com.google.guava" % "guava"         % Versions.guava

    val swaggerUI      = "org.webjars"      % "swagger-ui"    % Versions.swaggerUI

    val swaggerPlay2   = "io.swagger"      %% "swagger-play2" % Versions.swaggerPlay2

    val cronUtils      = "com.cronutils"    % "cron-utils"    % Versions.cron

    lazy val all = {
      internal.common   +:
      internal.catalog  +:
      typesafeConfig    +:
      catsCore          +:
      catsFree          +:
      swaggerUI         +:
      swaggerPlay2      +:
      cronUtils         +:
      (kafka.all ++ spark.all ++ play.all ++ kudu.all ++ hadoop.all ++ avro.all)
    }.map { Exclusions.slf4j }

  }

  object test {

    object hadoop {

      val client      = "org.apache.hadoop" % "hadoop-client"                     % Versions.hadoop % Test classifier "tests"

      val hdfs        = "org.apache.hadoop" % "hadoop-hdfs"                       % Versions.hadoop % Test classifier "tests"

      val hdfsTestJar = "org.apache.hadoop" % "hadoop-hdfs"                       % Versions.hadoop % Test classifier "tests" extra "type" -> "test-jar"

      val hdfsJar     = "org.apache.hadoop" % "hadoop-hdfs"                       % Versions.hadoop % Test extra "type" -> "test-jar"

      val miniCluster = "org.apache.hadoop" % "hadoop-minicluster"                % Versions.hadoop % Test

      val common      = "org.apache.hadoop" % "hadoop-common"                     % Versions.hadoop % Test classifier "tests" extra "type" -> "test-jar"

      val jobClient   = "org.apache.hadoop" % "hadoop-mapreduce-client-jobclient" % Versions.hadoop % Test classifier "tests"

      val all = Seq(client, hdfs, hdfsTestJar, hdfsJar, miniCluster, common, jobClient).map { Exclusions.hadoop }

    }

    object kudu {

      val client    = "org.apache.kudu" % "kudu-client" % Versions.kudu % Test classifier "tests"

      val clientJar = "org.apache.kudu" % "kudu-client" % Versions.kudu % Test classifier "tests" extra "type" -> "test-jar"

      val all = Seq(client, clientJar)

    }

    val logback       = "ch.qos.logback"          % "logback-classic"    % Versions.logback       % Test

    val playSpecs2    = "com.typesafe.play"      %% "play-specs2"        % Versions.play          % Test

    val scalaCheck    = "org.scalacheck"         %% "scalacheck"         % Versions.scalaCheck    % Test

    val scalaTest     = "org.scalatest"          %% "scalatest"          % Versions.scalaTest     % Test

    val scalactic     = "org.scalactic"          %% "scalactic"          % Versions.scalaTest     % Test

    val scalaTestPlay = "org.scalatestplus.play" %% "scalatestplus-play" % Versions.scalaTestPlay % Test

    val slf4j         = "org.slf4j"              % "slf4j-log4j12"       % Versions.slf4j         % Test

    val julSlf4j      = "org.slf4j"              % "jul-to-slf4j"        % Versions.slf4j         % Test

    val all = {
      logback       +:
      playSpecs2    +:
      scalaCheck    +:
      scalaTest     +:
      scalactic     +:
      scalaTestPlay +:
      (hadoop.all ++ kudu.all)
    }.map { Exclusions.slf4j } :+
      slf4j :+
      julSlf4j

  }


}
