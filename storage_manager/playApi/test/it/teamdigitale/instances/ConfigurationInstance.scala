package it.teamdigitale.instances

import com.typesafe.config.ConfigFactory

import play.api.Configuration

trait ConfigurationInstance {

  protected val configFile = "test.conf"

  protected val configuration = Configuration { ConfigFactory.load(configFile) }

}
