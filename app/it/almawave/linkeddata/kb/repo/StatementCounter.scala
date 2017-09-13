package it.almawave.linkeddata.kb.repo

import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler
import org.eclipse.rdf4j.model.Statement

class StatementCounter extends AbstractRDFHandler {

  var count = 0

  override def handleStatement(st: Statement) {
    count += 1
  }

  def counted(): Int = count

}