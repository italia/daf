object Versions {

  lazy val sparkVersion = "2.0.0"

  val playVersion = "2.5.14"

  val swaggerUiVersion = "3.0.10"

  val dafCatalogVersion = sys.env.getOrElse("CATALOG_MANAGER_VERSION", "1.0.0-SNAPSHOT")

  val dafSecurityVersion = sys.env.getOrElse("SECURITY_MANAGER_VERSION", "1.0.1-SNAPSHOT")
}
