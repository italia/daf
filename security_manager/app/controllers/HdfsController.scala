package controllers

import javax.inject.Inject

import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.gov.daf.securitymanager.service.{WebHDFSApiClient, WebHDFSApiProxy}
import it.gov.daf.securitymanager.service.utilities.RequestContext.execInContext
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.streams
import play.api.libs.ws.{StreamedBody, WSClient}
import play.api.mvc._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext



class HdfsController @Inject()(ws: WSClient, webHDFSApiClient:WebHDFSApiClient, webHDFSApiProxy:WebHDFSApiProxy) extends Controller {


  private val logger = Logger(this.getClass.getName)

  //----------------SECURED API---------------------------------------

  def retriveACL(path:String) = Action.async { implicit request =>
    execInContext[Future[Result]]("retriveACL") { () =>

      webHDFSApiClient.getAclStatus(path).map {
        case Right(r) => Ok(r)
        case Left(l) => new Status(l.httpCode).apply(l.jsValue.toString())
      }

    }
  }


  def callWebHdfs(path:String) = Action.async (wehdfsBodyParser) { implicit request =>

    execInContext[Future[Result]]("callWebHdfs") { () =>

      val queryString: Map[String, String] = request.queryString.map { case (k,v) => k -> v.mkString}

      logger.debug("Request:" + request)

      webHDFSApiProxy.callHdfsService(request.method, path, queryString, None).flatMap {
        case Right(r) =>  if( r.httpCode == 307 && r.locationHeader.nonEmpty)
                            request.method match{
                              case "GET" => callGetFileService(r.locationHeader.get)
                              case "PUT" => callPutFileService(r.locationHeader.get,request.body)
                            }
                          else
                            Future.successful{Ok(r.jsValue)}

        case Left(l) => Future.successful{ new Status(l.httpCode).apply(l.jsValue.toString()) }
      }

    }

  }


  private def callPutFileService(location:String, requestBody:SourceWrapper):Future[Result]={

    logger.debug("callPutFileService")

    ws.url(location)
      .withBody(StreamedBody(requestBody.source))
      .execute("PUT")
      .map{ resp =>

        logger.debug("exiting callPutFileService")

        if(resp.status < 400 )
          Ok("File succesfully uploaded")
        else
          Status(resp.status).apply(resp.body)
      }

  }


  private def callGetFileService(location:String):Future[Result]={

    logger.debug("callGetFileService")

    ws.url(location).withMethod("GET").stream().map { strResponse =>

        val response = strResponse.headers
        val body = strResponse.body

        logger.debug("exiting callGetFileService")

        if (response.status < 300) {

          val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/octet-stream")

          response.headers.get("Content-Length") match {
            case Some(Seq(length)) =>
              Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), Some(contentType)))
            case _ =>
              Ok.chunked(body).as(contentType)
          }
        } else {
          Status(response.status)
        }
    }

  }


  private def wehdfsBodyParser: BodyParser[SourceWrapper] = BodyParser { reqHd =>

    streams.Accumulator.source[ByteString].map{ source =>
      Right(SourceWrapper(source))
    }

  }

  case class SourceWrapper(source:Source[ByteString,_])


/*
  def callWebHdfs(path:String) = Action.async { implicit request =>
    execInContext[Future[Result]]("callWebHdfs") { () =>


      //val appo = request.body.asMultipartFormData.get.files
      val queryString: Map[String, String] = request.queryString.map { case (k,v) => k -> v.mkString}

      webHDFSApiProxy.callHdfsService(request.method, path, queryString).map {
        case Right(r) => Ok(r.jsValue)
        //if(r.httpCode==307 && r.locationHeader.nonEmpty)
        //call

        case Left(l) => new Status(l.httpCode).apply(l.jsValue.toString())
      }

    }
  }*/

  /*
  def callWebHdfsAction2[A](action: Action[A], path:String)= Action.async(action.parser) { request =>

    execInContext[Future[Result]]("callWebHdfs2") { () =>

      println("Calling action")

      val queryString: Map[String, String] = request.queryString.map { case (k, v) => k -> v.mkString }


      webHDFSApiProxy.callHdfsService(request.method, path, queryString).flatMap {

        case Right(r) => println("request.method "+request.method);println("r.httpCode "+r.httpCode);println("r.locationHeader "+r.locationHeader);
          if (request.method == "PUT" && r.httpCode == 307 && r.locationHeader.nonEmpty) {
            val vr = request.body.asInstanceOf[WR]
            println("weeeeeeeeeeeeeeee " + request)

            ws.url(r.locationHeader.get)
              .withBody(StreamedBody(vr.sou))//.withRequestTimeout(5.minutes)
              .execute("PUT")
              .map(e => Ok("uploaded"))

        } else
          Future {
            Ok(r.jsValue)
          }



        case Left(l) => println("request.method "+request.method);println("r.httpCode "+l.httpCode);println("r.locationHeader "+l.locationHeader);Future {
          new Status(l.httpCode).apply(l.jsValue.toString())
        }
      }

    }(requestHeader = request)

  }*/


/*
    def callWebHdfsAction2[A](action: Action[A], path:String)= Action.async(action.parser) { request =>

      execInContext[Future[Result]]("callWebHdfs2") { () =>

        println("Calling action")

        val queryString: Map[String, String] = request.queryString.map { case (k, v) => k -> v.mkString }


        webHDFSApiProxy.callHdfsService(request.method, path, queryString).flatMap {

          case Right(r) => println("request.method "+request.method);println("r.httpCode "+r.httpCode);println("r.locationHeader "+r.locationHeader);if (request.method == "PUT" && r.httpCode == 307 && r.locationHeader.nonEmpty) {
            request.body.asInstanceOf[WR]
            println("weeeeeeeeeeeeeeee " + request)
            val request2 = new WrappedRequest[A](request) {
              override def headers = request.headers.add("location" -> r.locationHeader.get)
            }
            action(request2)
          } else
            Future {
              Ok(r.jsValue)
            }



          case Left(l) => println("request.method "+request.method);println("r.httpCode "+l.httpCode);println("r.locationHeader "+l.locationHeader);Future {
            new Status(l.httpCode).apply(l.jsValue.toString())
          }
        }

      }(requestHeader = request)

    }*/

    //lazy val parser = parse.default
  //}


  //def callWebHdfs2(path:String) = callWebHdfsAction2(uploaderAction,path)


  /*
  def forward: BodyParser[Any] = BodyParser { reqHd =>

    val location = reqHd.headers.get("location")

    println("location ----->"+location)

    streams.Accumulator.source[ByteString].mapFuture { source =>
      println("in acc <--")
      if(location.nonEmpty) {
        println("in acc if <--")
        val request = ws.url(location.get)
        request
          .withBody(StreamedBody(source))
          .execute("PUT")
          .map(Right.apply)
      }else
        Future{Right("skip")}

    }

  }*/

  /*
  def uploaderAction = Action(forward) { req =>
    Ok("Uploaded")
  }*/






  /*
    def upload = Action(parse.) { request =>

      request.body.
        file("picture").map { picture =>
        import java.io.File
        val filename = picture.filename
        val contentType = picture.contentType
        picture.ref.moveTo(new File(s"/tmp/picture/$filename"))
        Ok("File uploaded")
      }.getOrElse {
        Redirect(routes.Application.index).flashing(
          "error" -> "Missing file")
      }
    }*/

  /*
    type FilePartHandler[A] = FileInfo => Accumulator[ByteString, Source[ByteString,_]]

    def handleFilePartAsFile: FilePartHandler[Source[ByteString,_]] = {
      case FileInfo(partName, filename, contentType) =>


        val perms = java.util.EnumSet.of(OWNER_READ, OWNER_WRITE)
        val attr = PosixFilePermissions.asFileAttribute(perms)
        val path = Files.createTempFile("multipartBody", "tempFile", attr)
        val file = path.toFile
        val fileSink = FileIO.toFile(file)
        val accumulator = Accumulator(fileSink)
        accumulator.map { case IOResult(count, status) =>
          FilePart(partName, filename, contentType, file)
        }(play.api.libs.concurrent.Execution.defaultContext)
    }*/

}