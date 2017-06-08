package it.gov.daf.catalogmanager.utilities.datastructures

import catalog_manager.yaml.{DatasetCatalog, InputSrc, ConversionField}

case class ConvSchema (
                        name: String,
                        isStd: Boolean = false,
                        theme: String,
                        cat: Seq[String] = Seq(),
                        groupOwn: String,
                        owner: String,
                        src: InputSrc,
                        dataSchema: DatasetCatalog,
                        stdSchemaUri: Option[String],
                        reqFields: Seq[ConversionField] = Seq(),  //StdSchema - those are the fields of the input dataset that map to the StdSchema ones.
                        custFields: Seq[String] = Seq() //StdSchema - those are the list of field names of the input dataset that are in addition to the StdSchema ones.
                      )