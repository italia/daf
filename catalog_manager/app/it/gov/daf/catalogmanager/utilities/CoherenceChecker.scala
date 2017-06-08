package it.gov.daf.catalogmanager.utilities

import catalog_manager.yaml.{MetaCatalog, MetadataCat}


import scala.util.Try


/**
  * Created by fabiana on 19/05/17.
  */

object CoherenceChecker {

  /**
    * Check the coherence between a convSchema and a stdSchema
    * In particuar, for each required filed in the standard schema check whther the field is contained into the convSchema
    * @param ordinary
    * @param stdMetacatalog
    */
  def checkCoherenceSchemas(ordinary: MetaCatalog, stdMetacatalog: MetaCatalog): Try[Boolean] = Try{
    val reqStdFields: Seq[String] = stdMetacatalog.dataschema.get.avro.get
      .fields.get
      .map(x => x.name)

    val convFields = ordinary.dataschema.get.flatSchema.get.map(x => x.name) //dataschema.get.flatSchema.get.map(x => x.).toSet

    reqStdFields.forall(x => convFields.contains(x))
  }




}
