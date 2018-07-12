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

import daf.dataset.FileDatasetParams
import daf.filesystem.ParquetFileFormat
import daf.util.HDFSBase

class PhysicalDatasetControllerHDFSSpec extends HDFSBase {

  var physicalC: PhysicalDatasetController = _

  "PhysicalDatasetController" should "get a dataset from hdfs" in {

    physicalC = new PhysicalDatasetController(getSparkSession, "master.kudu")

    val hdfsParams = FileDatasetParams(pathParquet, ParquetFileFormat)

    val dfParquet = physicalC.get(hdfsParams)
    dfParquet shouldBe 'Success
    dfParquet.get.count() should be > 0L
    dfParquet.foreach(_.show())

  }

}
