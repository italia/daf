object Versions {

  lazy val defaultVersion = sys.env.getOrElse("COMMON_VERSION", "1.0-alpha.1")

  // internal

  lazy val common = sys.env.getOrElse("COMMON_VERSION", defaultVersion)

  // external

  lazy val betterFiles    = "2.17.1"

  lazy val catsCore       = "1.1.0"

  lazy val dockerJava     = "3.0.13"

  lazy val doobie         = "0.6.0-M1"

  lazy val guava          = "16.0.1"

  lazy val h2             = "1.4.197"

  lazy val hadoop         = "2.6.0-cdh5.12.0"

  lazy val hbase          = "1.2.0-cdh5.12.0"

  lazy val hive           = "1.1.0-cdh5.12.0"

  lazy val kudu           = "1.4.0-cdh5.12.0"

  lazy val livyClient     = "0.6.0-incubating-SNAPSHOT"

  lazy val log4j          = "2.9.1"

  lazy val logback        = "1.2.3"

  lazy val play           = "2.5.14"

  lazy val scalaJ         = "2.3.0"

  lazy val scalaTest      = "3.0.4"

  lazy val scalaTestPlay  = "2.0.1"

  lazy val simpleClient   = "0.1.0"

  lazy val slf4j          = "1.7.25"

  lazy val spark          = "2.2.0.cloudera1"

  lazy val sparkAvro      = "4.0.0"

  lazy val swaggerPlay2   = "1.5.3"

  lazy val swaggerUI      = "3.0.7"

  lazy val typesafeConfig = "1.3.1"

}
