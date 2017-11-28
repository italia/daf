package it.teamdigitale


import it.teamdigitale.baseSpec.OpenTSDBbase

class OpenTSDBControllerSpec extends OpenTSDBbase {


  "OpenTSDBController" should s"read data from table $metric in opentsdb" in {
    val openTSDBController = new OpenTSDBController(getSparkSession)

    val df = openTSDBController.readData(metric, Map.empty[String, String], None)
    //This enable to test that the result is of type Success
    df.foreach(_.show())
    df shouldBe 'Success
    df.get.count() should equal(99L)
  }





}

