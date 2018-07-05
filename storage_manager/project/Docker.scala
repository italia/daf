import com.typesafe.sbt.packager.docker.{ Cmd, CmdLike }
import sbt.file

object Docker {

  private val target = Versions.choose(
    whenSnapshot = "test",
    whenRelease  = "prod"
  )

  val base: String = "anapsix/alpine-java:8_jdk_unlimited"

  val repository: Option[String] = Versions.choose(
    whenSnapshot = Some { "nexus.teamdigitale.test"   },
    whenRelease  = Some { "nexus.daf.teamdigitale.it" }
  )

  val ports = Seq(9000)

  val mappings = Seq(
    file { s"cert/$target/jssecacerts" } -> "jssecacerts",
    file { s"conf/$target/daf.conf"    } -> "daf.conf",
    file { "conf/base.conf"            } -> "base.conf"
  )

  private val updateKrb5Commands = Seq(
    Cmd("RUN", "apk update && apk add bash krb5-libs krb5"),
    Cmd("RUN", "ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf")
  )

  private val addKeystore = Seq(
    Cmd("COPY", "jssecacerts", "/opt/jdk/jre/lib/security/")
  )

  private val addConfiguration = Seq(
    Cmd("COPY", "daf.conf",         "conf/daf.conf"),
    Cmd("COPY", "base.conf",        "conf/base.conf")
  )

  def updateEnvironment(key: String, value: String)(commands: Seq[CmdLike]) = commands :+ Cmd("ENV", key, value)

  def updateConfiguration(commands: Seq[CmdLike]) = commands ++ addConfiguration

  def appendSecurity(commands: Seq[CmdLike]) = commands.span {
    case Cmd("FROM", _) => true
    case _              => false
  } match {
    case (Nil, tail)  => tail
    case (head, tail) => head ++ updateKrb5Commands ++ addKeystore ++ tail
  }

  def entryPoint(artifactName: String) = Seq(s"bin/$artifactName", s"-Dconfig.file=conf/mnt/daf.conf")
}

