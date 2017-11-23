package it.gov.daf.catalogmanager.listeners

/**
  * Created by ale on 13/06/17.
  */

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import net.caoticode.dirwatcher.FSListener
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.Future



class DirManager() extends FSListener {
  import java.nio.file.Path
  import scala.concurrent.ExecutionContext.Implicits.global

  override def onCreate(ref: Path): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val wsClient = AhcWSClient()

    val name = ref.getParent.getFileName.toString
    println(name)
    val uri: Option[String] = IngestionUtils.datasetsNameUri.get(name)
    val logicalUri = URLEncoder.encode(uri.get)
    println(logicalUri)

    call(wsClient)
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

    def call(wsClient: WSClient): Future[Unit] = {
      wsClient.url("http://localhost:9001/ingestion-manager/v1/add-datasets/" + logicalUri)
        //.withHeaders("content-type" -> "multipart/form-data")
        .post(
        Source(FilePart("upfile", name, None, FileIO.fromPath(ref)) :: List())).map { response =>
        val statusText: String = response.statusText
        println(s"Got a response $statusText")
      }
    }
    println(s"created $ref")
  }


  override def onDelete(ref: Path): Unit = println(s"deleted $ref")
  override def onModify(ref: Path): Unit = println(s"modified $ref")
}

