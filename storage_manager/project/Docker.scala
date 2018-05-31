import com.typesafe.sbt.packager.Keys.dockerCommands
import com.typesafe.sbt.packager.docker.{ Cmd, CmdLike }
import sbt.Keys.name

object Docker {

  val base: String = "anapsix/alpine-java:8_jdk_unlimited"

  val repository: Option[String] = Some { "nexus.daf.teamdigitale.it" }

  val ports = Seq(9000)

  private val updateKrb5Commands = Seq(
    Cmd("RUN", "apk update && apk add bash krb5-libs krb5"),
    Cmd("RUN", "ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf")
  )

  def appendKrb5(commands: Seq[CmdLike]) = commands.span {
    case Cmd("FROM", _) => true
    case _              => false
  } match {
    case (Nil, tail)  => tail
    case (head, tail) => head ++ updateKrb5Commands ++ tail
  }

//  def commands = dockerCommands.map { appendKrb5 }

  def entryPoint(artifactName: String) = Seq(s"bin/$artifactName", "-Dconfig.file=conf/production.conf")
}

