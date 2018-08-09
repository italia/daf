package it.gov.daf.ftp

import java.util.Properties

import com.jcraft.jsch.{ChannelSftp, JSch, Session, SftpException}

import scala.util.Try

class SftpHandler(
  user: String,
  pwd: String,
  host: String
) extends AutoCloseable {

  private val client = new JSch()
  client.setKnownHosts(System.getProperty("user.home") + "/.ssh/known_hosts")

  private val session: Session = client.getSession(user, host)

  private val config = new Properties()
  config.setProperty("StrictHostKeyChecking", "no")
  config.setProperty(
    "PreferredAuthentications",
    "publickey,keyboard-interactive,password"
  )

  session.setConfig(config)
  session.setPassword(pwd)
  session.connect(5000)

  private val channel = session.openChannel("sftp")
  channel.connect()

  private val sftp = channel.asInstanceOf[ChannelSftp]

  def disconnect() = {
    sftp.disconnect()
    channel.disconnect()
    session.disconnect()
  }

  /**
   *
   * @param path it can be
   *             - in case of a single relative path such as pippo or pippo/pluto it creates the folders starting from the user working dir
   *             - in case of an absolute path it removes the user working directory and create the requested folders
   * @return
   */
  def mkdir(path: String): Try[String] = {
    //remove the working directory in case of absolute path
    workingDir()
      .map { workDir =>
        if (path.contains(workDir)) path.replace(workDir, "").tail //remove the first slash
        else path
      }
      //create the folders
      .flatMap(relPath => mkdir(relPath.split("/").toList))
  }

  /**
   *
   * @param folders given a  list of folders creates the path
   * @return
   */
  private def mkdir(folders: List[String]): Try[String] = {
    val wd = workingDir()
    val path = folders.foldLeft(Try("")) { (acc, folder) =>
      //create the folder
      val op = Try(sftp.cd(folder)).recover {
        case e: SftpException =>
          sftp.mkdir(folder)
          sftp.cd(folder)
      }
      //concatenate the path
      op.flatMap(_ => acc)
        .map(v => s"$v/$folder")
    }

    //go back to the home
    wd.flatMap(cd)

    path
  }

  def cd(path: String): Try[String] = Try {
    sftp.cd(path)
    path
  }

  def rmdir(path: String) = Try {
    val checkedPath = if (path.startsWith("/")) {
      workingDir().map { workDir =>
        path.replace(workDir, "").tail
      }
    } else Try(path)

    checkedPath.map { p =>
      sftp.rmdir(p)
      p
    }
  }

  def workingDir(): Try[String] = Try(sftp.pwd())

  def chown(uid: Int, path: String): Try[Unit] = Try {
    sftp.chown(uid, path)
  }

  override def close(): Unit = disconnect()
}
