package it.almawave.linkeddata.kb.repo

import java.io.File
import org.eclipse.rdf4j.rio.Rio
import java.io.InputStream
import java.io.FileInputStream
import org.eclipse.rdf4j.rio.RDFFormat

// a simple main as an example
object MainRDFRepository extends App {

  val repo = RDFRepository.memory()
  repo.start()

//  repo.io.importFrom("dist/data/ontologies")

  val rdf_file = new File("dist/data/ontologies/foaf/foaf.rdf")
  val prefix = "testing_foaf_00"
  val context = "http://example/testing_foaf_00/"

  repo.io.addFile("testing_foaf_00.rdf", rdf_file, prefix, context)

  val rdf_doc = Rio.parse(new FileInputStream(rdf_file), "", RDFFormat.RDFXML)
  repo.store.add(rdf_doc, "http://example_02/")

  println("\n\n#### RESULTS")
  val results = repo.sparql.query("SELECT * WHERE { ?subject a ?concept }")

  results
    .get
    .toStream
    .foreach {
      item =>
        println(item)
    }

  println("\n\n#### CONTEXTS")
  repo.store.contexts().get
    .foreach { ctx => println(ctx) }

  repo.stop()
}