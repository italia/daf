import sbt._

object Dependencies {

  object compile {

    object internal {

      lazy val common = "it.gov.daf" %% "common" % Versions.common

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

    object hbase {

      val client       = "org.apache.hbase" % "hbase-client"        % Versions.hbase

      val protocol     = "org.apache.hbase" % "hbase-protocol"      % Versions.hbase

      val hadoopCompat = "org.apache.hbase" % "hbase-hadoop-compat" % Versions.hbase

      val server       = "org.apache.hbase" % "hbase-server"        % Versions.hbase

      val common       = "org.apache.hbase" % "hbase-common"        % Versions.hbase

      val all = Seq(client, protocol, hadoopCompat, server, common).map { Exclusions.hbase }

    }

    object spark {

      val core      = "org.apache.spark" %% "spark-core"      % Versions.spark

      val sql       = "org.apache.spark" %% "spark-sql"       % Versions.spark

      val streaming = "org.apache.spark" %% "spark-streaming" % Versions.spark

      val avro      = "com.databricks"   %% "spark-avro"      % Versions.sparkAvro

      val all = Seq(core, sql, streaming, avro).map {  Exclusions.spark }

    }

    object kudu {

      val client = "org.apache.kudu"  % "kudu-client" % Versions.kudu

      val spark2 = "org.apache.kudu" %% "kudu-spark2" % Versions.kudu

      val all = Seq(client, spark2)

    }

    val hiveJdbc            = "org.apache.hive"  % "hive-jdbc"            % Versions.hive

    val doobie              = "org.tpolecat"    %% "doobie-core"          % Versions.doobie

    val typesafeConfig      = "com.typesafe"     % "config"               % Versions.typesafeConfig

    val catsCore            = "org.typelevel"   %% "cats-core"            % Versions.catsCore

    val guava               = "com.google.guava" % "guava"                % Versions.guava

    val livyClient          = "org.apache.livy"  % "livy-client-http"     % Versions.livyClient

    val swaggerUI           = "org.webjars"      % "swagger-ui"           % Versions.swaggerUI

    val swaggerPlay2        = "io.swagger"      %% "swagger-play2"        % Versions.swaggerPlay2

    val simpleClient        = "io.prometheus"    % "simpleclient"         % Versions.simpleClient

    val simpleClientHotspot = "io.prometheus"    % "simpleclient_hotspot" % Versions.simpleClient

    val simpleClientCommon  = "io.prometheus"    % "simpleclient_common"  % Versions.simpleClient

    val scalaJ              = "org.scalaj"      %% "scalaj-http"          % Versions.scalaJ

    lazy val all = {
      internal.common     +:
      doobie              +:
      hiveJdbc            +:
      typesafeConfig      +:
      catsCore            +:
      livyClient          +:
      swaggerUI           +:
      swaggerPlay2        +:
      simpleClient        +:
      simpleClientHotspot +:
      simpleClientCommon  +:
      scalaJ              +:
      (spark.all ++ play.all ++ kudu.all ++ hadoop.all ++ hbase.all)
    }.map { Exclusions.slf4j }

  }

  object test {

    object hbase {

      val server           = "org.apache.hbase" % "hbase-server"        % Versions.hbase % Test classifier "tests"

      val common           = "org.apache.hbase" % "hbase-common"        % Versions.hbase % Test classifier "tests"

      val util             = "org.apache.hbase" % "hbase-testing-util"  % Versions.hbase % Test classifier "tests" exclude("org.apache.hadoop", "hadoop-minicluster")

      val hadoopCompat     = "org.apache.hbase" % "hbase-hadoop-compat"  % Versions.hbase % Test classifier "tests"

      val hadoopCompatJar  = "org.apache.hbase" % "hbase-hadoop-compat"  % Versions.hbase % Test classifier "tests" extra "type" -> "test-jar"

      val hadoop2CompatJar = "org.apache.hbase" % "hbase-hadoop2-compat" % Versions.hbase % Test classifier "tests" extra "type" -> "test-jar"

      val all = Seq(server, common, util, hadoopCompat, hadoopCompatJar, hadoop2CompatJar).map { Exclusions.hadoop }

    }

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

    object spark {

      val hive = "org.apache.spark" %% "spark-hive" % Versions.spark % Test

      val all = Seq(hive)

    }

    val logback       = "ch.qos.logback"          % "logback-classic"    % Versions.logback       % Test

    val playSpecs2    = "com.typesafe.play"      %% "play-specs2"        % Versions.play          % Test

    val scalaTest     = "org.scalatest"          %% "scalatest"          % Versions.scalaTest     % Test

    val scalactic     = "org.scalactic"          %% "scalactic"          % Versions.scalaTest     % Test

    val scalaTestPlay = "org.scalatestplus.play" %% "scalatestplus-play" % Versions.scalaTestPlay % Test

    val betterFiles   = "com.github.pathikrit"  %% "better-files"        % Versions.betterFiles   % Test

    val dockerJava    = "com.github.docker-java" % "docker-java"         % Versions.dockerJava    % Test

    val slf4j         = "org.slf4j"              % "slf4j-log4j12"       % Versions.slf4j         % Test

    val julSlf4j      = "org.slf4j"              % "jul-to-slf4j"        % Versions.slf4j         % Test

    val h2            = "com.h2database"         % "h2"                  % Versions.h2            % Test

    val all = {
      logback       +:
      playSpecs2    +:
      scalaTest     +:
      scalactic     +:
      scalaTestPlay +:
      betterFiles   +:
      dockerJava    +:
      dockerJava    +:
      h2            +:
      (hbase.all ++ hadoop.all ++ spark.all ++ kudu.all)
    }.map { Exclusions.slf4j } :+
    slf4j :+
    julSlf4j

  }


}
