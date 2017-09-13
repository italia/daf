package it.almawave.linkeddata.kb.repo.managers

import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File
import java.net.URLDecoder
import org.eclipse.rdf4j.rio.Rio
import java.io.FileInputStream
import java.nio.file.Paths
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import it.almawave.linkeddata.kb.utils.TryHandlers._
import it.almawave.linkeddata.kb.utils.RDF4JAdapters._
import java.nio.file.Files
import java.net.URI
import com.typesafe.config.Config
import java.nio.file.StandardCopyOption
import java.nio.file.Path
import it.almawave.linkeddata.kb.repo.RDFRepositoryBase

class RDFFileManager(kbrepo: RDFRepositoryBase) {

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  // TODO: refactorize configurations
  private val _conf = ConfigFactory.parseString("""
    import.formats = [ "owl", "rdf", "ttl", "nt" ]
  """)

  val default_format = RDFFormat.TURTLE

  /**
   * adding an RDF file (ontology/vocabulary)
   */
  def addFile(rdfName: String, rdfFileFromInput: File, prefix: String, context: String) {

    TryLog {

      val _context = URLDecoder.decode(context, "UTF-8")
      val format = Rio.getParserFormatForFileName(rdfName)
        .orElse(default_format)

      val fis = new FileInputStream(rdfFileFromInput.getAbsoluteFile)

      // adds the file as an RDF document
      val doc = Rio.parse(fis, "", format, context.toIRI)

      // trying to register a default prefix:namespace pair for context
      kbrepo.prefixes.add((prefix, context))
      // add the RDF document to the given context
      kbrepo.store.add(doc, context)

      fis.close()

      // CHECK: saving the file locally, if needed

    }(s"KB:RDF> cannot add RDF file: ${rdfFileFromInput}")

  }

  // TODO: add a configuration 
  def importFrom(rdf_folder: String) {

    val logger = LoggerFactory.getLogger(this.getClass)

    val base_path = Paths.get(rdf_folder).toAbsolutePath().normalize()

    logger.debug(s"KB:RDF> importing RDF from ${base_path.toAbsolutePath()}")

    this.list(rdf_folder, _conf.getStringList("import.formats"): _*) // TODO: configuration
      .foreach {
        uri =>

          // CHECK: how to put an ontology in the right context? SEE: configuration

          val format = Rio.getParserFormatForFileName(uri.toString()).get

          val rdf_doc = Rio.parse(uri.toURL().openStream(), uri.toString(), format)

          // adds all the namespaces from the file
          val doc_namespaces = rdf_doc.getNamespaces.map { ns => (ns.getPrefix, ns.getName) }.toList
          kbrepo.prefixes.add(doc_namespaces: _*)

          val meta = this.getMetadata(uri)

          if (meta.hasPath("prefix")) {

            // adds the default prefix/namespace pair for this document
            val prefix = meta.getString("prefix")
            val namespace = meta.getString("uri")

            logger.debug(s"\nadding ${prefix}:${namespace}")
            kbrepo.prefixes.add((prefix, namespace))

            val contexts_list = meta.getStringList("contexts")

            logger.debug(s"importing ${uri} in context ${contexts_list(0)}")
            logger.debug(s"(available contexts are: ${contexts_list.mkString(" | ")})")

            // adds the document to the contexts provided in .metadata
            kbrepo.store.add(rdf_doc, contexts_list: _*)
            kbrepo.store.add(rdf_doc) // also publish to the default context

          } else {
            logger.warn(s"skipping import of ${uri}: missing meta!")
          }

      }

  }

  def copy(source: Path, destination: Path) {
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
  }

  def list(base: String, ext: String*): Stream[URI] = {

    val base_path = Paths.get(base).toAbsolutePath().normalize()

    Files.walk(base_path).iterator().toStream
      .filter(_.toFile().isFile())
      .filter(_.toString().matches(s".*\\.(${ext.mkString("|")})"))
      .map(_.toUri().normalize())

  }

  // this method attemps to find a .metadata file related to the vocabulary
  def getMetadata(uri: URI): Config = {

    val file = Paths.get(uri)
    val dir = file.getParent
    val file_name = file.getFileName.toString().replaceAll("(.*)\\..*", "$1")
    val file_metadata = Paths.get(dir.toString(), s"${file_name}.metadata")

    var conf = ConfigFactory.empty()
    if (Files.exists(file_metadata)) {
      conf = conf.withFallback(ConfigFactory.parseURL(file_metadata.toUri().toURL()))
    }

    conf
  }
}