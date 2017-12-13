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
import java.io.Writer
import javax.inject.Singleton

import akka.util.ByteString
import io.prometheus.client._
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import play.api.http.HttpEntity
import play.api.mvc._


@Singleton
class MetricsController extends Controller {
  //export default jvm metrics
  DefaultExports.initialize()

  def index = Action {
    val samples = new StringBuilder()
    val writer = new WriterAdapter(samples)

    TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples())
    writer.close()

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Strict(ByteString(samples.toString), Some(TextFormat.CONTENT_TYPE_004))
    )
  }
}

class WriterAdapter(buffer: StringBuilder) extends Writer {
  override def write(charArray: Array[Char], offset: Int, length: Int): Unit = {
    val b = buffer.append(new String(new String(charArray, offset, length).getBytes("UTF-8"), "UTF-8"))
  }
  override def flush(): Unit = {}
  override def close(): Unit = {}
}