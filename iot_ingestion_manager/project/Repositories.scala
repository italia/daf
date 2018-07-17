import sbt._
import sbt.Keys.isSnapshot

object Repositories {

  val nexusUrl = Versions.choose(
    whenSnapshot = "http://nexus.teamdigitale.test:8081/repository/",
    whenRelease  = "http://nexus.daf.teamdigitale.it:8081/repository/"
  )

  def publish: Def.Initialize[Option[Resolver]] = isSnapshot {
    case true  => Some { "snapshots" at s"$nexusUrl/maven-snapshots/" }
    case false => Some { "releases"  at s"$nexusUrl/maven-releases/"  }
  }

  def credential = Credentials { Path.userHome / ".ivy2" / ".credentials" }

}
