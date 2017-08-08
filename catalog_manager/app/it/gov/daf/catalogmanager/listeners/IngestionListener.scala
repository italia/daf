package it.gov.daf.catalogmanager.listeners

/**
  * Created by ale on 12/06/17.
  */
import java.io.File
import javax.inject._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import it.gov.daf.catalogmanager.MetaCatalog
import it.gov.daf.catalogmanager.client.Catalog_managerClient
import net.caoticode.dirwatcher.DirWatcher
import play.Environment
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.{ExecutionContext, Future}

trait IngestionListener {
  def initialize(): Unit
  def stop(): Unit
  def addDirListener(metaCatalog: catalog_manager.yaml.MetaCatalog, logicalUri :String) :Unit
}

object IngestionUtils {
  val datasetsNameUri = scala.collection.mutable.Map[String,String]()
}

@Singleton
class IngestionListenerImpl @Inject() (appLifecycle: ApplicationLifecycle) extends IngestionListener {


  private var watcherMap =  scala.collection.mutable.Map[String,DirWatcher]()


  override def initialize(): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val myExecutionContext: ExecutionContext = system.dispatchers.lookup("contexts.ingestion-lookups")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val clientManager: AhcWSClient = AhcWSClient()
    val catalogManager = new Catalog_managerClient(clientManager)("http://localhost:9000")
    val metacalogs: Future[List[MetaCatalog]] = catalogManager.datasetcatalogs("test:test")
    metacalogs.map(catalogs => {
        val holders: Set[String] = catalogs.map(catalog  => {
          catalog.dcatapit.get.owner_org.get
        }).toSet

      val fileDirs = catalogs.map(x => {
        val org = x.dcatapit.get.owner_org.get
        val name = x.dcatapit.get.identifier.get //.value.get
        val logicalUri = x.operational.get.logical_uri.get
        IngestionUtils.datasetsNameUri += (name -> logicalUri)
        val orgDir: File = Environment.simple().getFile("data/org/" + org + "/" + name)
        if(!orgDir.exists()) {
          orgDir.mkdirs()
        }
      })

      holders.foreach( org => {
        val orgDir = Environment.simple().getFile("data/org/" + org)
        val watcher = new DirWatcher(system)
        watcher.watchFor(orgDir.getAbsolutePath, new DirManager())
        watcher.start()
        watcherMap += (org -> watcher)
      })
    })
  }

  override def stop(): Unit = {
    watcherMap.foreach(_._2.stop())
  }

  override def addDirListener(metaCatalog: catalog_manager.yaml.MetaCatalog, logicalUri :String): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    val org = metaCatalog.dcatapit.get.owner_org.get  //.value.get
    val name = metaCatalog.dcatapit.get.identifier.get //.value.get
    IngestionUtils.datasetsNameUri += (name -> logicalUri)
    val orgDir: File = Environment.simple().getFile("data/org/" + org + "/" + name)
    if(!orgDir.exists()) {
      orgDir.mkdirs()
    }
    val organizationDir = Environment.simple().getFile("data/org/" + org)
     watcherMap.get(org) match {
       case None => {
         val watcher = new DirWatcher(system)
         watcher.watchFor(organizationDir.getAbsolutePath, new DirManager())
         watcher.start()
         watcherMap += (org -> watcher)
       }
       case Some(_) => Logger.info("Watcher already added")
    }
  }

  // You can do this, or just explicitly call `hello()` at the end
  def start(): Unit = initialize()

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    stop()
    Future.successful(())
  }

  // Called when this singleton is constructed (could be replaced by `hello()`)
  start()
}
