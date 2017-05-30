package it.gov.daf.catalogmanager.utilities

import catalog_manager.yaml.MetaCatalog
//import it.gov.daf.catalogmanagerclient.model.MetaCatalog


import scala.util.Try

/**
  * Created by ale on 29/05/17.
  */

package object datastructures {

  def convertToConvSchema(schema: MetaCatalog) = Try{
    println("ale")
    ConvSchema(
       uri = schema.operational.get.logical_uri,
      name = schema.dcatapit.get.dct_title.get.`val`.get,
      isStd = (schema.operational.get.is_std.get > 0),
      theme = schema.dcatapit.get.dcat_theme.get.`val`.get,
      cat = schema.dcatapit.get.dct_subject.get.map(x => x.`val`.get),
      groupOwn = schema.operational.get.group_own.get,
      owner = schema.dcatapit.get.dct_rightsHolder.get.`val`.get,
      src = schema.operational.get.input_src.get,
      dataSchema = schema.dataschema.get,
      stdSchemaUri = Option(schema.operational.get.std_schema.get.std_uri.get),
      reqFields = schema.operational.get.std_schema.get.fields_conv.get,
      custFields = Seq() // TODO da togliere o popolare
    )
  }

  def convertToStdSchema(schema: MetaCatalog) = Try{

    if(schema.operational.get.logical_uri.get.size < 1)
      throw new RuntimeException("No uri associated to this schema")

    StdSchema(
            name = schema.dcatapit.get.dct_title.get.`val`.get,
            nameDataset = schema.dataschema.get.name,
            uri = schema.operational.get.logical_uri.get,
            theme = schema.dcatapit.get.dcat_theme.get.`val`.get,
            cat = schema.dcatapit.get.dct_subject.get.map(x => x.`val`.get),
            groupOwn = schema.operational.get.group_own.get,
            owner = schema.dcatapit.get.dct_rightsHolder.get.`val`.get,
            dataSchema = schema.dataschema.get
          )
  }

  object DatasetType extends Enumeration {
    val STANDARD = Value("std")
    val ORDINARY = Value("ord")
    val RAW = Value("raw")

    def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
  }

}
