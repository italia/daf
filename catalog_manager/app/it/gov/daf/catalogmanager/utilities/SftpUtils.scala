package it.gov.daf.catalogmanager.utilities
import catalog_manager.yaml.MetaCatalog
import net.schmizz.sshj.SSHClient


object SftpUtils {

  val user = ConfigReader.ingestionUser
  val ingestionPass = ConfigReader.ingestionPass

   def createDirs(metaCatalog: MetaCatalog) = {
     println("User : " + user)
     println("Ingestion : " + ingestionPass)
     var ssh = new SSHClient
     ssh.loadKnownHosts()
     ssh.connect("edge1")
     ssh.authPassword(user, ingestionPass)
     try {
       val sftp = ssh.newSFTPClient()
       try {
         sftp.mkdirs("/home/daf_ingestion/" + metaCatalog.operational.theme + "/" + metaCatalog.operational.subtheme + "/" + metaCatalog.dcatapit.name)
       } catch {
         case ex :Exception => println(ex.getMessage)
       }
       finally {
         sftp.close
       }
     } finally ssh.disconnect
   }

}
