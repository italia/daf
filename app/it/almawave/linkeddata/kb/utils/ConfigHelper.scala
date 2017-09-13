package it.almawave.linkeddata.kb.utils

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions

object ConfigHelper {

  private val options_render = ConfigRenderOptions.concise()
    .setComments(false)
    .setOriginComments(false)
    .setFormatted(true)
    .setJson(false)

  //  private val conf: Config = ConfigFactory.empty()

  def pretty(conf: Config) = conf.root().render(options_render)

}

