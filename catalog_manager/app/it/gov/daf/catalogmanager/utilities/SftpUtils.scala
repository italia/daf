package it.gov.daf.catalogmanager.utilities

import catalog_manager.yaml.MetaCatalog
import net.schmizz.sshj.SSHClient


object SftpUtils {

   def createDirs(metaCatalog: MetaCatalog) = {
     var ssh = new SSHClient
     ssh.loadKnownHosts()
     ssh.connect("edge1")
     ssh.authPassword("alessandro", "silviale7881")
     try {
       val sftp = ssh.newSFTPClient()
       try {
         sftp.mkdirs("/home/alessandro/" + metaCatalog.operational.theme + "/" + metaCatalog.operational.subtheme + "/" + metaCatalog.dcatapit.name)
       } catch {
         case ex :Exception => println(ex.getMessage)
       }
       finally {
         sftp.close
       }
     } finally ssh.disconnect
   }

}
