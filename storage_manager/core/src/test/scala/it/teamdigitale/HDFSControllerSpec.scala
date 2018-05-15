package it.teamdigitale


import it.teamdigitale.baseSpec.HDFSbase

import scala.util.Failure

class HDFSControllerSpec extends HDFSbase {

  var HDFSController: HDFSController = _

  override def beforeAll: Unit = {
    super.beforeAll()
    HDFSController = new HDFSController(sparkSession)
  }

  "A HDFS controller" should "get a dataset from hdfs when a path exists" in {

    val dfParquet = HDFSController.readData(pathParquet, "parquet", None)
    val dfAvro = HDFSController.readData(pathAvro, "avro",None)
    val dfCsv = HDFSController.readData(pathCsv, "csv", None)

    dfParquet shouldBe 'Success
    dfParquet.get.count() should be > 0L
    dfParquet.foreach(_.show())

    dfAvro shouldBe 'Success
    dfAvro.get.count() should be > 0L
    dfAvro.foreach(_.show())

    dfCsv shouldBe 'Success
    dfCsv.get.count() should be > 0L
    dfCsv.foreach(_.show())
  }

  it should "handle requests with wrong paths"in {
    val df = HDFSController.readData(s"wrongPath/test.parquet", "parquet")
    df shouldBe 'Failure
  }

  it should "handle requests with wrong format" in {
    val df = HDFSController.readData(s"wrongPath/test.parquet", "wrongFormat")
    df shouldBe 'Failure
    df === Failure(new IllegalArgumentException(s"Format wrongFormat is not implemented"))
  }

}

