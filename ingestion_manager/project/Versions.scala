object Versions {

  lazy val sparkVersion = "2.0.0"

  val playVersion = "2.5.14"

  val swaggerUiVersion = "3.0.10"

  val dafCatalogVersion = sys.env.get("CATALOG_MANAGER_VERSION").getOrElse("1.0-SNAPSHOT")
}
