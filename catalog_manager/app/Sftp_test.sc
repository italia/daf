import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient

import scala.util.Try


/*def createDirectories(groupOwn :String, theme :String, subtheme: String, filename :String) = Try {
  val ssh = new SSHClient
  ssh.loadKnownHosts()
  ssh.connect("edge1")
  ssh.authPassword("alessandro", "silviale7881")
  val sftp = ssh.newSFTPClient()
  val dirPath = "/home/" + groupOwn + "/" + theme + "/" + subtheme "/" + filename
  sftp.mkdirs("/home/alessandro/theme/subtheme/name")
}*/

val ssh = new SSHClient
ssh.loadKnownHosts()
ssh.connect("edge1")
ssh.authPassword("alessandro", "silviale7881")
val sftp = ssh.newSFTPClient()
//val dirPath = "/home/" + groupOwn + "/" + theme + "/" + subtheme "/" + filename
sftp.mkdirs("/home/alessandro/theme/subtheme/name")

/*
var ssh = new SSHClient
ssh.loadKnownHosts()
ssh.connect("edge1")
ssh.authPassword("alessandro", "silviale7881")
try {
  val sftp = ssh.newSFTPClient()
  try {
    sftp.mkdirs("/home/alessandro/theme/subtheme/name")
    //sftp.get("test_file", new FileSystemFile("/tmp"))
  } catch {
    case ex :Exception => println(ex.getMessage)
  }
  finally {
    sftp.close
  }
} finally ssh.disconnect
*/