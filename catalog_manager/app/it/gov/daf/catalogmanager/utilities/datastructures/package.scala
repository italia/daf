package it.gov.daf.catalogmanager.utilities

import catalog_manager.yaml.MetaCatalog
//import it.gov.daf.catalogmanagerclient.model.MetaCatalog


import scala.util.Try

/**
  * Created by ale on 29/05/17.
  */

package object datastructures {

 /* def convertToConvSchema(schema: MetaCatalog) = Try{
    println("ale")
    ConvSchema(
      name = schema.dcatapit.get.dct_title.get.value.get,
      isStd = schema.operational.get.is_std.get,
      theme = schema.dcatapit.get.dct_theme.get.value.get,
      cat = schema.dcatapit.get.dct_subject.get.map(x => x.value.get),
      groupOwn = schema.operational.get.group_own.get,
      owner = schema.dcatapit.get.dct_rightsHolder.get.value.get,
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
            name = schema.dcatapit.get.dct_title.get.value.get,
            nameDataset = schema.dataschema.get.avro.get.name,
            uri = schema.operational.get.logical_uri.get,
            theme = schema.dcatapit.get.dct_theme.get.value.get,
            cat = schema.dcatapit.get.dct_subject.get.map(x => x.value.get),
            groupOwn = schema.operational.get.group_own.get,
            owner = schema.dcatapit.get.dct_rightsHolder.get.value.get,
            dataSchema = schema.dataschema.get
          )
   } */

  object DatasetType extends Enumeration {
    val STANDARD = Value("standard")
    val ORDINARY = Value("ordinary")
    val RAW = Value("raw")

    def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
  }

}
