package it.almawave.linkeddata.kb.repo.managers

import scala.util.Try
import org.slf4j.LoggerFactory
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Model
import it.almawave.linkeddata.kb.utils.TryHandlers._
import it.almawave.linkeddata.kb.utils.RDF4JAdapters._
import it.almawave.linkeddata.kb.repo.RepositoryAction

/*
 * this component can be seen as an RDF datastore abstraction
 */
class RDFStoreManager(repo: Repository) {

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  def clear(contexts: String*) {

    RepositoryAction(repo) { conn =>

      if (contexts.size > 0) {
        conn.clear(contexts.toIRIList: _*)
      } else {
        // default clear
        conn.clear(null)
        conn.clear()
        // clear each context
        conn.clear(conn.getContextIDs.toList: _*)
      }
      conn.commit()

    }(s"KB:RDF> cannot clear contexts: ${contexts.mkString(", ")}")

  }

  def contexts(): Try[Seq[String]] = {

    val results = RepositoryAction(repo) { conn =>

      conn.getContextIDs.map { ctx => ctx.stringValue() }.toList

    }(s"KB:RDF> cannot retrieve contexts list")

    results
  }

  def size(contexts: String*): Try[Long] = {

    val size = RepositoryAction(repo) { conn =>

      if (contexts.size > 0)
        conn.size(contexts.toIRIList: _*)
      else {
        conn.size(null)
      }

    }(s"can't obtain size for contexts: ${contexts.mkString(" | ")}")

    size
  }

  def statements(s: Resource, p: IRI, o: Value, inferred: Boolean, contexts: String*) = {

    // CHECK: not efficient! (reference to stream head!)
    val results = RepositoryAction(repo) { conn =>

      conn.getStatements(null, null, null, false, contexts.toIRIList: _*).toStream

    }(s"cannot get statements for ${contexts.mkString(" | ")}")

    results

  }

  def add(doc: Model, contexts: String*) {

    RepositoryAction(repo) { conn =>

      conn.add(doc, contexts.toIRIList: _*)
      conn.commit()
      logger.debug(s"KB:RDF> ${doc.size()} triples was added to contexts ${contexts.mkString(" | ")}")

    }(s"KB:RDF> cannot add RDF data in ${contexts.mkString("|")}")

  }

  def remove(doc: Model, contexts: String*) {

    RepositoryAction(repo) { conn =>

      conn.remove(doc, contexts.toIRIList: _*)
      conn.commit()
      logger.debug(s"KB:RDF> ${doc.size()} triples was removed from contexts ${contexts.mkString(" | ")}")

    }(s"KB:RDF> cannot remove RDF data")

  }

}