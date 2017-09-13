package it.almawave.linkeddata.kb.virtuoso

import virtuoso.rdf4j.driver.VirtuosoRepository
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File
import org.eclipse.rdf4j.query.QueryLanguage
import it.almawave.linkeddata.kb.repo.RDFRepository

/*
 * testing local virtuoso connection, via JDBC
 * TODO: unit test / specs2 !
 */
object MainVirtuoso extends App {

  val repo = new VirtuosoRepository(s"jdbc:virtuoso://localhost:1111/charset=UTF-8/log_enable=2", "dba", "dba")
  repo.initialize()

  val conn = repo.getConnection

  val vf = conn.getValueFactory

  try {

    val ctx = vf.createIRI("http://dati.gov.it/onto/dcat-ap-it")

    conn.begin()
    conn.clear(ctx)

    val triples_before = conn.size(ctx)

    conn.add(
      new File("dist/data/ontologies/agid/DCAT-AP_IT/DCAT-AP_IT.owl"),
      "http://dati.gov.it/onto/",
      RDFFormat.RDFXML,
      ctx)
    conn.commit()

    val triples_after = conn.size(ctx)

    println(s"added ${triples_after - triples_before} triples to virtuoso")

  } catch {
    case ex: Exception =>
      println("problems adding triples to virtuoso")
  }

  val query = s"""
    SELECT DISTINCT * 
    FROM <> 
    FROM <http://dati.gov.it/onto/dcat-ap-it>
    WHERE {
      GRAPH <http://dati.gov.it/onto/dcat-ap-it> {
        ?s ?p ?o
      }
    }  
    """

  val results = conn
    .prepareTupleQuery(QueryLanguage.SPARQL, query)
    .evaluate()

  var i = 0
  while (results.hasNext()) {
    i += 1
    println(results.next())
  }

  println(s"\n${i} results")
  conn.close()

  // ---------------

  val kb = RDFRepository.virtuoso()
  kb.start()
  kb.io.importFrom("dist/data/ontologies")
  kb.stop()

}