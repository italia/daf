package it.gov.daf.ingestion.metacatalog

import it.gov.daf.catalogmanager.{GroupAccess, StorageHdfs, StorageInfo}

object MetaCatalogDefault {
  def storageInfo() = new StorageInfo(
    hdfs = Some(StorageHdfs(
      name="hdfs_daf",
      path = None,
      param = None)),
    kudu = None,
    hbase = None,
    textdb = None,
    mongo = None)

  def ingPipeline() = List(" ")


}
