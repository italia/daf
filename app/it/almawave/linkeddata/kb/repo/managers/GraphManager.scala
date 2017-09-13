package it.almawave.linkeddata.kb.repo.managers

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.query.QueryLanguage
import org.slf4j.LoggerFactory
import org.eclipse.rdf4j.repository.Repository
import scala.util.Try
import it.almawave.linkeddata.kb.utils.TryHandlers.TryLog
import java.lang.Long
import it.almawave.linkeddata.kb.repo.RepositoryAction

/*
 *  IDEA: an helper class for CRUD using SPARQL
 *  the connection should be managed externally, ideally with a connection pool
 *  SEE: https://www.w3.org/TR/sparql11-update/#create
 */
protected class GraphManager(repo: Repository) {

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  val MAX_EXEC_TIME = 120000
  val INCLUDE_INFERRED = false

  def exists(IRIref: IRI): Try[Boolean] = {

    val query = s"""
      ASK { 
        GRAPH <${IRIref}> {}
      }
      """

    TryLog {

      logger.debug(s"SPARQL> executing query:\n${query}")

      // CHECK: conn.hasStatement(null, null, null, false, IRIref)

      val conn = repo.getConnection
      val result = conn
        .prepareBooleanQuery(QueryLanguage.SPARQL, query)
        .evaluate()
      conn.close()

      result

    }(s"SPARQL> problems executing query:\n${query}")

  }

  def count(IRIref: IRI): Try[Long] = {

    val query = s"""
      SELECT (COUNT(?s) AS ?triples) { 
        GRAPH <${IRIref}> { ?s ?p [] }
      }
    """

    RepositoryAction(repo) { conn =>

      logger.debug(s"SPARQL> executing query:\n${query}")

      val update = conn.prepareTupleQuery(QueryLanguage.SPARQL, query)
      update.setMaxExecutionTime(MAX_EXEC_TIME)
      update.setIncludeInferred(INCLUDE_INFERRED)
      val result = update.evaluate().next()
      Long.parseLong(result.getBinding("triples").getValue.stringValue())

    }(s"SPARQL> cannot execute query:\n${query}")

  }

  // VERIFY
  def create(IRIref: IRI) = ???

  def drop(IRIref: IRI) = {

    val query = s"""
      DROP GRAPH <http://example/bookStore> 
    """

    RepositoryAction(repo) { conn =>

      logger.debug(s"SPARQL> executing query:\n${query}")

      conn
        .prepareUpdate(QueryLanguage.SPARQL, query.trim())
        .execute()

    }(s"SPARQL> cannot execute query:\n${query}")

  }

  def copy = ???
  def move = ???
  def add = ???

}