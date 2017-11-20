package it.gov.daf.play.modules

import java.io.File
import java.net.{URL, URLClassLoader}
import javax.inject.{Inject, Singleton}

import com.google.inject.AbstractModule
import org.slf4j.LoggerFactory
import play.api.{Configuration, Environment}

/**
  * This module adds the configuration for
  *  - hadoop_conf_dir
  *  - hbase_conf_dir
  *
  *  to a play project that uses hadoop components.
  *
  * This modules needs that a valid path for the two folders to be mounted
  *
  * @param conf
  */
@Singleton
class ClouderaModule @Inject()(environment: Environment, conf: Configuration) extends AbstractModule {
  private val log = LoggerFactory.getLogger(this.getClass.getName)
  log.debug(s"init ${this.getClass.getName} module from daf-play-modules")

  def addPath(dir: String): Unit = {
    val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    method.setAccessible(true)
    method.invoke(Thread.currentThread().getContextClassLoader, new File(dir).toURI.toURL)
  }

  override def configure() = {
    conf.getString("hadoop_conf_dir").foreach(addPath)
    conf.getString("hbase_conf_dir").foreach(addPath)
  }
}
