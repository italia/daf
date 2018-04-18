package it.gov.daf.sso

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

import it.gov.daf.securitymanager.service.utilities.ConfigReader

import scala.concurrent.Future
import scala.sys.process.{Process, ProcessLogger}
import play.api.libs.concurrent.Execution.Implicits._

object WebHDFSLogin {

  private val HADOOP_URL = ConfigReader.hadoopUrl

  def loginF(usrName:String,pwd:String):Future[Either[String,String]] = {
    Future{ login(usrName,pwd) }
  }

  def login(usrName:String,pwd:String):Either[String,String] = {

    val out = new StringBuilder("OUT:")
    val err = new StringBuilder("ERR:")
    val result = new StringBuilder

    try {

      val logger = ProcessLogger(
        (o: String) => {out.append(s"$o\n");()},
        (e: String) => {err.append(s"$e\n");()} )


      val pb = Process(s"timeout 5 ./script/kb_init.sh $usrName $HADOOP_URL") // Process should hang: command timeout needed

      val bos = new ByteArrayOutputStream()
      val exitCode = pb #< new ByteArrayInputStream(s"$pwd\n".toCharArray.map(_.toByte)) #> bos ! logger

      result.append(new String(bos.toByteArray, StandardCharsets.UTF_8))

      if (exitCode == 0) {
        Right(result.toString().split("\r?\n").filter(_.startsWith("Set-Cookie")).head.replaceFirst("Set-Cookie:", "").trim)
      } else if (exitCode == 1)
        Left(s"Error in kinit script  \n$result\n$out\n$err")
      else
        Left(s"Error in kinit script: timeout  \n$result\n$out\n$err")

    } catch {
      case t: Throwable => Left(s"Error in during webHDFS init \n$result\n$out\n$err")
    }

  }


}
