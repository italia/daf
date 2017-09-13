package check.rdf4j

import it.almawave.linkeddata.kb.repo.RDFRepositoryBase
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.StringReader
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQueryResultHandler
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerFactory
import org.eclipse.rdf4j.IsolationLevels
import org.eclipse.rdf4j.model.Resource

/**
 * extract a proper test case showing various combinations, from this example
 */
object CheckInferences extends App {

  val mem = new MemoryStore
  val inferencer = new ForwardChainingRDFSInferencer(mem)

  val _config = new ForwardChainingRDFSInferencerConfig()

  new ForwardChainingRDFSInferencerFactory()
    .getSail(_config)

  val repo: Repository = new SailRepository(inferencer)
  //  val repo: Repository = new SailRepository(mem) NO EMPLOYEE!

  repo.initialize()

  val conn = repo.getConnection

  val res = """
    
    @prefix ex: <http://example.org/> .
    
    ex:person_01 a ex:Person ;
      ex:name "first person" 
    .
    ex:person_02 a ex:Employee ;
      ex:name "second person" 
    .
    
    ex:Person a owl:Class .
    
    ex:Employee a owl:Class;
      rdfs:subClassOf ex:Person .
    
  """

  val vf = conn.getValueFactory

  // conn.add(new StringReader(res), "", RDFFormat.TURTLE)
  // conn.add(new StringReader(res), "", RDFFormat.TURTLE, null)
  // conn.add(new StringReader(res), "", RDFFormat.TURTLE, Array[Resource](): _*)
  conn.add(new StringReader(res), "", RDFFormat.TURTLE, vf.createIRI("http://example.org/persons/"))

  val query = """
    PREFIX ex: <http://example.org/> 
    SELECT *
    WHERE {
      ?subject a ex:Person .
      ?subject ex:name ?name .
      # inferences ..............................
      # ?subject a / rdfs:subClassOf* ex:Person .
      {OPTIONAL {
          GRAPH ?graph { ?subject a ex:Person . }
      }} 
      UNION {
        BIND(<http://inferred/> AS ?graph_from)
      } 
    }  
  """

  val results = conn.prepareTupleQuery(QueryLanguage.SPARQL, query)
    .evaluate()

  while (results.hasNext()) {
    val bs = results.next()
    println(bs)
  }

  conn.close()

  repo.shutDown()

}