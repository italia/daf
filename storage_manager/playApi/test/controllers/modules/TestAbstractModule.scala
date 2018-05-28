package controllers.modules

import com.google.inject.AbstractModule

import org.pac4j.play.store.{ PlayCacheSessionStore, PlaySessionStore }

abstract class TestAbstractModule extends AbstractModule {

  protected def sessionStoreInstance: PlaySessionStore

  def sessionStore: PlaySessionStore = sessionStoreInstance

  def configure(): Unit = {
    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore]).asEagerSingleton()
  }

}
