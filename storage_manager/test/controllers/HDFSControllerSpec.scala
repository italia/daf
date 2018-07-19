/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import daf.util.HDFSBase

import scala.util.Failure

class HDFSControllerSpec extends HDFSBase {

  var HDFSController: HDFSController = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    HDFSController = new HDFSController(sparkSession)
  }

  "A HDFS controller" should "get a dataset from hdfs when a path exists" in {

    val dfParquet = HDFSController.readData(pathParquet, "parquet", None)
    val dfAvro = HDFSController.readData(pathAvro, "avro", None)
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
    val df = HDFSController.readData(s"wrongPath/test.parquet", "parquet", None)
    df shouldBe 'Failure
  }

  it should "handle requests with wrong format" in {
    val df = HDFSController.readData(s"wrongPath/test.parquet", "wrongFormat", None)
    df shouldBe 'Failure
    df === Failure(new IllegalArgumentException("Format wrongFormat is not implemented"))
  }

}

