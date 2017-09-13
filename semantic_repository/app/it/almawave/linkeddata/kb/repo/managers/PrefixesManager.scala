package it.almawave.linkeddata.kb.repo.managers

import scala.util.Try
import org.eclipse.rdf4j.model.vocabulary._
import org.eclipse.rdf4j.repository.Repository
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.utils.TryHandlers._
import it.almawave.linkeddata.kb.utils.RDF4JAdapters._
import it.almawave.linkeddata.kb.repo.RepositoryAction

class PrefixesManager(repo: Repository) {

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  def clear() = {

    RepositoryAction(repo) { conn =>

      conn.clearNamespaces()

    }(s"KB:RDF> error while removing namespaces!")

  }

  def add(namespaces: (String, String)*) {

    RepositoryAction(repo) { conn =>

      namespaces.foreach { pair => conn.setNamespace(pair._1, pair._2) }
      conn.commit()

    }(s"KB:RDF> cannot add namespaces: ${namespaces}")

  }

  def remove(namespaces: (String, String)*) {

    RepositoryAction(repo) { conn =>

      namespaces.foreach { pair => conn.setNamespace(pair._1, pair._2) }
      conn.commit()

    }(s"KB:RDF> cannot remove namespaces: ${namespaces}")

  }

  // get prefixes
  def list(): Try[Map[String, String]] = {

    val results = RepositoryAction(repo) { conn =>

      conn.getNamespaces.toList
        .map { ns => (ns.getPrefix, ns.getName) }
        .toMap

    }("cannot retrieve a list of prefixes")

    results

  }

  val DEFAULT = Map(
    OWL.PREFIX -> OWL.NAMESPACE,
    RDF.PREFIX -> RDF.NAMESPACE,
    RDFS.PREFIX -> RDFS.NAMESPACE,
    DC.PREFIX -> DC.NAMESPACE,
    FOAF.PREFIX -> FOAF.NAMESPACE,
    SKOS.PREFIX -> SKOS.NAMESPACE,
    XMLSchema.PREFIX -> XMLSchema.NAMESPACE,
    FN.PREFIX -> FN.NAMESPACE,
    "doap" -> DOAP.NAME.toString(), // SEE: pull request
    "geo" -> GEO.NAMESPACE,
    SD.PREFIX -> SD.NAMESPACE)

}