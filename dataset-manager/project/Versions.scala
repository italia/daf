object Versions {

  val playVersion = "2.5.14"

  val swaggerUiVersion = "3.0.10"

  val betterFilesVersion = "2.17.1"

  val dafCommonVersion = sys.env.get("COMMON_VERSION")
                            .getOrElse("1.0.0-SNAPSHOT")
}
