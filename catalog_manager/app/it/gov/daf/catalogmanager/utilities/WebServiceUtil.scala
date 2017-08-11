package it.gov.daf.catalogmanager.utilities

import java.io.File
import java.net.URLEncoder

import catalog_manager.yaml.Credentials
import org.apache.commons.net.util.Base64
import play.api.mvc.Request

//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.asynchttpclient.AsyncHttpClientConfig
import play.api.libs.ws.WSConfigParser
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClientConfig}
import play.api.{Configuration, Environment, Mode}
import base64.Decode.{ urlSafe => fromBase64UrlSafe }

/**
  * Created by ale on 11/05/17.
  */

object WebServiceUtil {

  val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
    """
      |ws.followRedirects = true
    """.stripMargin))

  // If running in Play, environment should be injected

  val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

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


  def readCredentialFromRequest( request:Request[Any]) :Credentials ={

    val auth = request.headers.get("authorization")

    val userAndPass = new String(Base64.decodeBase64(auth.get.split(" ").drop(1).head.getBytes)).split(":")

    Credentials( Option(userAndPass(0)), Option(userAndPass(1)) )

  }

}
