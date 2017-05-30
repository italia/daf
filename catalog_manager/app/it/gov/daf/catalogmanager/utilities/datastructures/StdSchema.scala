package it.gov.daf.catalogmanager.utilities.datastructures

import catalog_manager.yaml.DatasetCatalog


case class StdSchema (
                       name: String = "",
                       nameDataset: String,
                       uri: String,
                       stdSchemaName: String = "",  //to be deleted
                       theme: String,
                       cat: Seq[String] = Seq(),
                       groupOwn: String,
                       owner: String,
                       dataSchema: DatasetCatalog
                     )