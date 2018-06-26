import java.util.Properties

import com.jcraft.jsch.{ChannelSftp, JSch, Session, SftpException}

private val client = new JSch()

//client.setKnownHosts(System.getProperty("user.home") + "/.ssh/known_hosts")

private val session: Session = client.getSession("d_ale", "daf.teamdigitale.it")

private val config = new Properties()
config.setProperty("StrictHostKeyChecking", "no")
config.setProperty(
  "PreferredAuthentications",
  "publickey,keyboard-interactive,password"
)

println("ale")

session.setConfig(config)
session.setPassword("SilviAle7881")
println("ale")

try {
  session.connect(1000)
} catch {
  case ex: Exception => println(ex.getMessage)
}
println("ale")

private val channel = session.openChannel("sftp")
channel.connect()

private val sftp = channel.asInstanceOf[ChannelSftp]

println(sftp.pwd())

