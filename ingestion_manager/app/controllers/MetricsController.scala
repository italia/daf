package controllers

import java.io.Writer
import javax.inject.Singleton

import akka.util.ByteString
import io.prometheus.client._
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import play.api.http.HttpEntity
import play.api.mvc._

import scala.util.Try

@Singleton
class MetricsController extends Controller {
  //export default jvm metrics
  DefaultExports.initialize()

  val indexCounter = {
    val metric = Counter.build()
      .name("my_test_request_total")
      .help("total requests count").create()

    Try {
      CollectorRegistry.defaultRegistry.unregister(metric)
      metric.register()
    }

    metric
  }

  def index = Action {

    indexCounter.inc()

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
    buffer ++= new String(new String(charArray, offset, length).getBytes("UTF-8"), "UTF-8")
  }
  override def flush(): Unit = {}

  override def close(): Unit = {}
}
