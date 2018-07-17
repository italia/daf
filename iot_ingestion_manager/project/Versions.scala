object Versions {

  lazy val defaultVersion = sys.env.getOrElse("STORAGE_MANAGER_VERSION", "1.0.2-SNAPSHOT")

  lazy val isSnapshot = Versions.defaultVersion.endsWith("SNAPSHOT")

  // internal

  lazy val common = sys.env.getOrElse("COMMON_VERSION", "1.0.9-SNAPSHOT")

  // external

  lazy val avro           = "1.7.5"

  lazy val avroBijection  = "0.9.6"

  lazy val catsCore       = "1.1.0"

  lazy val guava          = "16.0.1"

  lazy val hadoop         = "2.6.0-cdh5.12.0"

  lazy val kudu           = "1.4.0-cdh5.12.0"

  lazy val logback        = "1.2.3"

  lazy val play           = "2.5.14"

  lazy val scalaJ         = "2.3.0"

  lazy val scalaTest      = "3.0.4"

  lazy val scalaTestPlay  = "2.0.1"

  lazy val slf4j          = "1.7.25"

  lazy val spark          = "2.2.0.cloudera1"

  lazy val sparkAvro      = "4.0.0"

  lazy val swaggerPlay2   = "1.5.3"

  lazy val swaggerUI      = "3.0.7"

  lazy val typesafeConfig = "1.3.1"

  def choose[A](whenSnapshot: => A, whenRelease: => A): A = if (isSnapshot) whenSnapshot else whenRelease

}
