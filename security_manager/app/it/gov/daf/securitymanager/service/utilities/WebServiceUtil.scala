package it.gov.daf.securitymanager.service.utilities

import java.io.File
import java.net.URLEncoder

import it.gov.daf.common.authentication.Authentication
import org.apache.commons.net.util.Base64
import play.api.libs.json._
import play.api.mvc.Request

//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.asynchttpclient.AsyncHttpClientConfig
import play.api.libs.ws.WSConfigParser
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClientConfig}
import play.api.{Configuration, Environment, Mode}

/**
  * Created by ale on 11/05/17.
  */

object WebServiceUtil {
  /*
  val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
    """
      |ws.followRedirects = true
    """.stripMargin))

  // If running in Play, environment should be injected

  val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

  Authentication(Configuration.load(Environment.simple()),null)

  val parser = new WSConfigParser(configuration, environment)
  val config = new AhcWSClientConfig(wsClientConfig = parser.parse())
  val builder = new AhcConfigBuilder(config)
  val logging = new AsyncHttpClientConfig.AdditionalChannelInitializer() {
    override def initChannel(channel: io.netty.channel.Channel): Unit = {
      channel.pipeline.addFirst("log", new io.netty.handler.logging.LoggingHandler("debug"))
    }
  }
  val ahcBuilder = builder.configure()
  ahcBuilder.setHttpAdditionalChannelInitializer(logging)
  val ahcConfig = ahcBuilder.build()
*/

  def buildEncodedQueryString(params: Map[String, Any]): String = {
    val encoded = for {
      (name, value) <- params if value != None
      encodedValue = value match {
        case Some(x)         => URLEncoder.encode(x.toString, "UTF8")
        case x               => URLEncoder.encode(x.toString, "UTF8")
      }
    } yield name + "=" + encodedValue

    encoded.mkString("?", "&", "")
  }


  def readCredentialFromRequest( request:Request[Any] ) :(Option[String],Option[String]) ={

    val auth = request.headers.get("authorization")
    val authType = auth.get.split(" ")(0)

    if( authType.equalsIgnoreCase("basic") ){

      val userAndPass = new String(Base64.decodeBase64(auth.get.split(" ").drop(1).head.getBytes)).split(":")
      ( Option(userAndPass(0)), Option(userAndPass(1)) )

    }else if( authType.equalsIgnoreCase("bearer") ) {

      val user:Option[String] = Option( Authentication.getClaims(request).get.get("sub").get.toString )
      println("JWT user:"+user)
      (user , None)

    }else
      throw new Exception("Authorization header not found")


    //val userAndPass = if (auth.get.contains(" ")) new String(Base64.decodeBase64(auth.get.split(" ").drop(1).head.getBytes)).split(":")
    //                  else new String(Base64.decodeBase64(auth.get.getBytes)).split(":")


  }

  def cleanDquote(in:String): String = {
    in.replace("\"","").replace("[","")replace("]","")
  }

  def getMessageFromJsError(error:JsError): String ={

    val jsonError = JsError.toJson(error)

    if( (jsonError \ "obj").toOption.isEmpty )
      jsonError.value.foldLeft("ERRORS--> "){ (s: String, pair: (String, JsValue)) =>
        s + "field: "+pair._1 +" message:"+ (pair._2 \\ "msg")(0).toString + "  "
      }
    else
      cleanDquote( (( (jsonError \ "obj")(0) \ "msg").getOrElse(JsArray(Seq(JsString(" ?? "))))(0) ).get.toString() )

  }

  /*
  def getMessageFromCkanError(error:JsValue): String ={


    val errorMsg = (error \ "error").getOrElse(JsString("can't retrive error") )

    val ckanError = errorMsg.as[JsObject].value.foldLeft("ERRORS: "){ (s: String, pair: (String, JsValue)) =>
      s + "<< field: "+pair._1 +"  message: "+ cleanDquote(pair._2.toString()) + " >>   "}

    ckanError

  }*/

}
