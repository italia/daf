package it.gov.daf.play.filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import org.pac4j.core.config.Config
import org.pac4j.play.filters.SecurityFilter
import org.pac4j.play.store.PlaySessionStore
import play.api.Configuration
import play.libs.concurrent.HttpExecutionContext

@Singleton
class DafSecurityFilter @Inject() (
  mat: Materializer,
  conf: Configuration,
  playSessionStore: PlaySessionStore,
  config: Config,
  ec: HttpExecutionContext
) extends SecurityFilter(mat, conf, playSessionStore, config, ec)
