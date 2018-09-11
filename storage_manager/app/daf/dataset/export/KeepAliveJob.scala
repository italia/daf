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

package daf.dataset.export

import org.apache.livy.{ Job, JobContext }
import org.apache.spark.sql.SparkSession

/**
  * Livy `Job` for keeping the spark session alive. A small stream of values will be streamed to the server and back to
  * the driver.
  */
class KeepAliveJob extends Job[Long] {

  private def doNothing(any: Any): Unit = {  }

  private def runJob(session: SparkSession) = session.createDataset(1 to 10)(session.implicits.newIntEncoder).foreach { doNothing(_) }

  override def call(jobContext: JobContext) = {
    val start = System.currentTimeMillis()
    runJob { jobContext.sqlctx().sparkSession }
    System.currentTimeMillis() - start
  }

}

object KeepAliveJob {

  def init = new KeepAliveJob

}