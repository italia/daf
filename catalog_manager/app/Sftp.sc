import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
//import net.schmizz.sshj.xfer.FileSystemFile


val ssh = new SSHClient
ssh.loadKnownHosts()
ssh.connect("edge1")
ssh.authPassword("alessandro", "XXXXXXXXXXXXX")

try {
  val sftp = ssh.newSFTPClient
  try {
    sftp.mkdirs("/home/alessandro/theme/subtheme/name")
    //sftp.get("test_file", new FileSystemFile("/tmp"))
  } catch {
    case ex :Exception => println(ex.getMessage)
  }
  finally sftp.close
} finally ssh.disconnect




/*import java.util
import com.jcraft.jsch._


val jsch = new JSch

try {
      val session = jsch.getSession("alessandro", "edge1", 22)
      session.setConfig("StrictHostKeyChecking", "no")
      session.setPassword("silviale7881")
      session.connect()
      val channel = session.openChannel("sftp")
      channel.connect()
      val sftpChannel = channel.asInstanceOf[ChannelSftp]
      val tests = List("alessandro", "theme", "subtheme", "filename")
      val bs = tests.fold("/home")((dirs, dir ) => {
        println(dirs)
        val ls: util.Vector[_] = sftpChannel.ls(dirs)
        println(ls)
        val dd = dirs + "/" + dir
        println(dd)
        //sftpChannel.mkdir(dd)
        dd
      })
      //val test: util.Vector[_] = sftpChannel.ls("/home/alessandro")
      //test.toArray.foreach(println(_))
      //sftpChannel.get("remotefile.txt", "localfile.txt")
      sftpChannel.exit()
      session.disconnect()
    } catch {
      case e: JSchException =>
        e.printStackTrace()
      case e: SftpException =>
        e.printStackTrace()
    }
*/