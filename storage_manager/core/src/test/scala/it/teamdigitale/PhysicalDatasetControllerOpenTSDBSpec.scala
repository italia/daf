package it.teamdigitale

import it.teamdigitale.baseSpec.OpenTSDBbase
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class PhysicalDatasetControllerOpenTSDBSpec extends OpenTSDBbase {

  var physicalC: PhysicalDatasetController = _

  "PhysicalDatasetController" should "get a dataset from opentsdb" in {

    physicalC = new PhysicalDatasetController(super.getSparkSession, "master.kudu")

    val opentsdbParams = Map(
      "protocol" -> "opentsdb",
      "metric" -> super.metric
    )
    val df = physicalC.get(opentsdbParams)
    df.foreach(_.limit(10).show())
    df shouldBe 'Success
    df.get.count() should equal(99L)
  }
}
