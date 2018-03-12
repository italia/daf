package it.teamdigitale.config

import com.typesafe.config.Config

object RichConfig{

implicit class RichConfig(val configuration: Config) {

  def getStringOrException(path: String): String = {
    if (configuration.hasPath(path))
      configuration.getString(path)
    else throw new RuntimeException(s"Missing path $path")
  }
  def getOptionalString(path: String): Option[String] = {
    if (configuration.hasPath(path))
      Some(configuration.getString(path))
    else None
  }


}

}
