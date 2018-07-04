import sbt._

object Resolvers {

  val cloudera = "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

  val daf      = "daf repo" at "http://nexus.daf.teamdigitale.it:8081/repository/maven-public/"

  val all = Seq(
    Resolver.mavenLocal,
    Resolver.sonatypeRepo("releases"),
    cloudera,
    daf
  )

}
