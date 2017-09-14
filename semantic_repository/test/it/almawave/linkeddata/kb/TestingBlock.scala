package it.almawave.linkeddata.kb

import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.repository.sail.SailRepository
import play.Logger
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.model.Resource
import java.net.URL
import org.eclipse.rdf4j.rio.RDFFormat
import scala.collection.mutable.ListBuffer
import it.almawave.linkeddata.kb.repo.RepositoryAction

// esempio per testing costrutti try/log etc
object TestingBlock extends App {

  implicit val logger = Logger.underlying()
  val repo: Repository = new SailRepository(new MemoryStore)
  repo.initialize()

  val results = RepositoryAction(repo) {

    conn =>

      val vf = conn.getValueFactory

      conn.add(
        new URL("http://xmlns.com/foaf/spec/index.rdf"), "",
        RDFFormat.RDFXML,
        vf.createIRI("http://xmlns.com/foaf/0.1/"))

      logger.info("this is a generic log")

      // TESTING ERRORS: throw new RuntimeException("some problem here!")

      val res = conn.getContextIDs
      val buffer = ListBuffer[Resource]()

      while (res.hasNext()) {
        buffer += res.next()
      }

      buffer.toList

  }("there was an error...")

  val contexts: List[String] = results.get

  println("RESULTS")
  println(contexts.mkString(" | "))

  repo.shutDown()
}
