package it.teamdigitale

import it.teamdigitale.baseSpec.HDFSbase

class PhysicalDatasetControllerHDFSSpec extends HDFSbase {

  var physicalC: PhysicalDatasetController = _

  "PhysicalDatasetController" should "get a dataset from hdfs" in {

    physicalC = new PhysicalDatasetController(getSparkSession, "master.kudu")

    val hdfsParams = Map(
      "protocol" -> "hdfs",
      "path" -> super.pathParquet,
      "format" -> "parquet"
    )
    val dfParquet = physicalC.get(hdfsParams)
    dfParquet shouldBe 'Success
    dfParquet.get.count() should be > 0L
    dfParquet.foreach(_.show())

  }

}
