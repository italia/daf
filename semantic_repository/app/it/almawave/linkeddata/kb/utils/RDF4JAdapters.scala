package it.almawave.linkeddata.kb.utils

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.rio.RDFFormat
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.OutputStream
import org.eclipse.rdf4j.repository.RepositoryResult
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.sail.memory.model.MemLiteral
import java.net.URI
import org.eclipse.rdf4j.sail.memory.model.MemIRI
import org.eclipse.rdf4j.model.impl.SimpleIRI
import org.eclipse.rdf4j.sail.memory.model.MemBNode
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.IRI

object RDF4JAdapters {

  implicit class StringContextAdapter(context: String) {
    def toIRI(implicit vf: ValueFactory = SimpleValueFactory.getInstance): IRI = {
      vf.createIRI(context)
    }
  }

  implicit class StringContextListAdapter(contexts: Seq[String]) {
    def toIRIList(implicit vf: ValueFactory = SimpleValueFactory.getInstance): Seq[IRI] = {
      contexts.map { cx => vf.createIRI(cx) }
    }
  }

  def prettyPrint(doc: Model, out: OutputStream, format: RDFFormat = RDFFormat.TURTLE) = ???

  implicit class RepositoryResultIterator[T](result: RepositoryResult[T]) extends Iterator[T] {
    def hasNext: Boolean = result.hasNext()
    def next(): T = result.next()
  }

  implicit class TupleResultIterator(result: TupleQueryResult) extends Iterator[BindingSet] {
    def hasNext: Boolean = result.hasNext()
    def next(): BindingSet = result.next()
  }

  implicit class BindingSetMapAdapter(bs: BindingSet) {

    def toMap(): Map[String, Object] = {

      val names = bs.getBindingNames
      names.map { n =>

        val binding = bs.getBinding(n)
        val name = binding.getName
        val value = binding.getValue match {
          case literal: MemLiteral => literal.stringValue()
          case iri: MemIRI         => new URI(iri.stringValue())
          case iri: SimpleIRI      => new URI(iri.stringValue())
          case bnode: MemBNode     => bnode.toString()
          case other               => other.toString()
        }

        (name, value)
      }.toMap
    }

  }

}