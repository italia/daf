package it.almawave.linkeddata.kb.repo

import org.eclipse.rdf4j.repository.RepositoryConnection
import org.slf4j.Logger
import scala.util.Failure
import scala.util.Try
import org.eclipse.rdf4j.repository.Repository
import scala.util.Success

/*
   * this could be useful for simplifying code: 
   * 	+ default connection handling (open/close)
   * 	+ default transaction handling
   */
object RepositoryAction {

  def apply[R](repo: Repository)(conn_action: (RepositoryConnection => Any))(msg_err: String)(implicit logger: Logger) = {

    // NOTE: we could imagine using a connection pool here
    val _conn = repo.getConnection

    _conn.begin()

    val results: Try[R] = try {

      val success = Success(conn_action(_conn))
      _conn.commit()
      success.asInstanceOf[Try[R]]

    } catch {

      case ex: Throwable =>
        val failure = Failure(ex)
        _conn.rollback()
        logger.info(msg_err)
        failure

    }

    _conn.close()

    results
  }

}