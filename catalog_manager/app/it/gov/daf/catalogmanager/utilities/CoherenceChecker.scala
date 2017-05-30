package it.gov.daf.catalogmanager.utilities

import it.gov.daf.catalogmanager.utilities.datastructures.{ConvSchema, StdSchema}
import it.gov.daf.catalogmanagerclient.model.{DatasetCatalog, MetaCatalog}

/**
  * Created by fabiana on 19/05/17.
  */

object CoherenceChecker {

  /**
    * Check the coherence between a convSchema and a stdSchema
    * In particuar, for each required filed in the standard schema check whther the field is contained into the convSchema
    * @param convSchema
    * @param stdSchema
    */
  def checkCoherenceSchemas(convSchema: ConvSchema, stdSchema: StdSchema): Boolean = {
    val reqStdFields: Seq[String] = stdSchema.dataSchema
      .fields.get
      .filter(_.metadata.get.required.get == 1)
      .map(x => x.name)

    val convFields = convSchema.reqFields.map(x => x.field_std).toSet

    reqStdFields.forall(x => convFields.contains(x))
  }




}
