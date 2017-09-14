package it.almawave.linkeddata.kb.repo

import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.nio.file.Paths
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.slf4j.LoggerFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import it.almawave.linkeddata.kb.utils.RDF4JAdapters.StringContextAdapter
import it.almawave.linkeddata.kb.utils.TryHandlers.TryLog
import virtuoso.rdf4j.driver.VirtuosoRepository
import it.almawave.linkeddata.kb.repo.managers.RDFFileManager
import it.almawave.linkeddata.kb.repo.managers.RDFStoreManager
import it.almawave.linkeddata.kb.repo.managers.PrefixesManager
import it.almawave.linkeddata.kb.repo.managers.SPARQLManager
import scala.concurrent.Future
import it.almawave.linkeddata.kb.utils.TryHandlers.FutureWithLog
import scala.util.Try

object RDFRepository {

  val logger = LoggerFactory.getLogger(this.getClass)

  def remote(endpoint: String) = {
    new RDFRepositoryBase(new SPARQLRepository(endpoint, endpoint))
  }

  def memory() = {

    val mem = new MemoryStore
    val repo: Repository = new SailRepository(mem)
    new RDFRepositoryBase(repo)

  }

  // TODO: config
  def memory(dir_cache: String) = {

    val dataDir = Paths.get(dir_cache).normalize().toAbsolutePath().toFile()
    if (!dataDir.exists())
      dataDir.mkdirs()
    val mem = new MemoryStore()
    mem.setDataDir(dataDir)
    mem.setSyncDelay(1000L)
    mem.setPersist(false)
    mem.setConnectionTimeOut(1000) // TODO: set a good timeout!

    // IDEA: see how to trace statements added by inferencing
    // CHECK val inferencer = new DedupingInferencer(new ForwardChainingRDFSInferencer(new DirectTypeHierarchyInferencer(mem)))
    // SEE CustomGraphQueryInferencer

    val repo = new SailRepository(mem)
    new RDFRepositoryBase(repo)
  }

  // VERIFY: virtuoso jar dependencies on maven central
  def virtuoso() = {
    // TODO: externalize configurations
    // TODO: add a factory for switching between dev / prod
    val host = "localhost"
    val port = 1111
    val username = "dba"
    val password = "dba"

    val repo = new VirtuosoRepository(s"jdbc:virtuoso://${host}:${port}/charset=UTF-8/log_enable=2", username, password)
    new RDFRepositoryBase(repo)
  }

  /* DISABLED */
  def solr() {

    //    val index = new SolrIndex()
    //    val sailProperties = new Properties()
    //    sailProperties.put(SolrIndex.SERVER_KEY, "embedded:")
    //    index.initialize(sailProperties)
    //    val client = index.getClient()
    //
    //    val memoryStore = new MemoryStore()
    //    // enable lock tracking
    //    org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true)
    //    val lucenesail = new LuceneSail()
    //    lucenesail.setBaseSail(memoryStore)
    //    lucenesail.setLuceneIndex(index)
    //
    //    val repo = new SailRepository(lucenesail)

  }

}

// TODO: refactorization using trait!!
trait RDFRepository

/**
 *
 * IDEA: use an implicit connection
 * TODO: provide a connection pool
 * TODO: add an update method (remove + add) using the same connection/transaction
 *
 * CHECK: finally (handle connection to be closed) and/or connection pool
 * 	the idea could be encapsulating default behaviours in Try{} object as much as possible
 *  SEE (example): https://codereview.stackexchange.com/questions/79267/scala-trywith-that-closes-resources-automatically
 *
 * TODO: import codebase of Configuration wrapper
 *
 * IDEA: default usage of TryLog, wrapper for external Java API
 */
class RDFRepositoryBase(repo: Repository) {

  //  val logger = Logger.underlying()

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  // CHECK: providing custom implementation for BN

  private var conf = ConfigFactory.empty()

  def configuration(configuration: Config) = {
    conf = conf.withFallback(configuration)
  }

  def configuration(): Config = conf

  // checking if the repository is up.
  def isAlive(): Try[Boolean] = {

    TryLog {

      if (!repo.isInitialized()) repo.initialize()
      repo.getConnection.close()
      repo.shutDown()
      true

    }("repository is not reachable!")

  }

  def start() = {

    TryLog {

      if (!repo.isInitialized())
        repo.initialize()

    }(s"KB:RDF> cannot start repository!")

  }

  def stop() = {

    TryLog {

      if (repo.isInitialized())
        repo.shutDown()

    }(s"KB:RDF> cannot stop repository!")

  }

  val prefixes = new PrefixesManager(repo)

  val store = new RDFStoreManager(repo)

  val sparql = new SPARQLManager(repo)

  val io = new RDFFileManager(this)

}
