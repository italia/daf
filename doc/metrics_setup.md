# Metrics Setup

To monitor our micro-services we can use:

1. [prometheus-client-java](https://github.com/prometheus/client_java) to export metrics in prometheus
2. [jolokia-jmx](https://github.com/rhuss/jolokia), to export metrics to influx-db and elastic

Thus this document explains how to instrument your application for both formats

## Prometheus java client

In order to add Prometheus metrics exporter in a play application we need to:

1. add the library dependencies

```sbt

libraryDependencies ++= Seq(
  "io.prometheus" % "simpleclient" % "0.5.0",
  "io.prometheus" % "simpleclient_hotspot" % "0.5.0",
  "io.prometheus" % "simpleclient_common" % "0.5.0"
)
```

2. create a metrics controller that exposes the `/metrics` path.

```scala

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
    buffer ++= new String(new String(charArray, offset, length).getBytes("UTF-8"), "UTF-8")
  }
  override def flush(): Unit = {}
  override def close(): Unit = {}
}
```

3. add to the `routes` file

```
GET         /metrics                         controllers.MetricsController.index
```

4. go to `http://localhost:9000/metrics` if you run your service locally of to `http://service-url:9000/metrics` to see the metrics.

```
# HELP process_cpu_seconds_total Total user and system CPU time spent in seconds.
# TYPE process_cpu_seconds_total counter
process_cpu_seconds_total 9604.68
# HELP process_start_time_seconds Start time of the process since unix epoch in seconds.
# TYPE process_start_time_seconds gauge
process_start_time_seconds 1.535727275365E9
# HELP process_open_fds Number of open file descriptors.
# TYPE process_open_fds gauge
process_open_fds 341.0
# HELP process_max_fds Maximum number of open file descriptors.
# TYPE process_max_fds gauge
process_max_fds 1048576.0
# HELP process_virtual_memory_bytes Virtual memory size in bytes.
# TYPE process_virtual_memory_bytes gauge
process_virtual_memory_bytes 8.11216896E9
# HELP process_resident_memory_bytes Resident memory size in bytes.
# TYPE process_resident_memory_bytes gauge
process_resident_memory_bytes 1.634152448E9
# HELP jvm_info JVM version info
# TYPE jvm_info gauge
jvm_info{version="1.8.0_172-b11",vendor="Oracle Corporation",} 1.0
# HELP jvm_gc_collection_seconds Time spent in a given JVM garbage collector in seconds.
# TYPE jvm_gc_collection_seconds summary
jvm_gc_collection_seconds_count{gc="G1 Young Generation",} 15.0
jvm_gc_collection_seconds_sum{gc="G1 Young Generation",} 0.691
jvm_gc_collection_seconds_count{gc="G1 Old Generation",} 0.0
jvm_gc_collection_seconds_sum{gc="G1 Old Generation",} 0.0
# HELP jvm_threads_current Current thread count of a JVM
# TYPE jvm_threads_current gauge
...
...
```

Prometheus supports: *Counters, Gauges, Summaries and Histograms* that can be used to setup additional metrics as explained [here](https://github.com/prometheus/client_java#instrumenting).
Examples of metrics:
- counters: can be used to count the number for request received
- gauges: can be used to count the number of user connected
- summaries: can be used to store summaries about latencies and size of payloads processed
- histograms: can be used to tracks size and number of events received in buckets

Check the documentation of [prometheus-java client](https://github.com/prometheus/client_java#instrumenting) for further details.

## Jolokia
In order to add metrics to the deployed micro-services we uses [jolokia-jmx](https://github.com/rhuss/jolokia).
Jolokia is a way to access JMX MBeans remotely. It is different from JSR-160 connectors in that it is an agent-based approach which uses JSON over HTTP for its communication in a REST-stylish way.

### Setup

#### sbt-jolokia

1. add `Resolver.bintrayRepo("jtescher", " sbt-plugin-releases")` to resolvers settings

```sbt-jolokia

resolvers += Resolver.bintrayRepo("jtescher", " sbt-plugin-releases")

```

To steup [sbt-jolokia](https://github.com/jtescher/sbt-jolokia) for play do:
2. add `jolokia.sbt` in the `project` folder

```sbt

addSbtPlugin("com.jatescher" % "sbt-jolokia" % "1.1.0")

```

3. To use the Jolokia settings in your project, add the Jolokia auto-plugin to your project.

```
enablePlugins(Jolokia)
```

4. Customize the setting for jolokia port

```sbt
.settings(
  jolokiaPort := "7000"
)
```

5. Add the exposed port to `sbt-native-packager`

```sbt

dockerExposedPorts := Seq(7000)

```

6. run `sbt stage start` to run the service in pseudo-production

7. navigate to `https://127.0.0.1:7000/jolokia/` and you should see a response like

```json
{
   "request":{
      "type":"version"
   },
   "value":{
      "agent":"1.3.7",
      "protocol":"7.2",
      "config":{
         "maxDepth":"15",
         "discoveryEnabled":"true",
         "maxCollectionSize":"0",
         "agentId":"10.137.1.54-14997-6bf2d08e-jvm",
         "debug":"false",
         "agentType":"jvm",
         "historyMaxEntries":"10",
         "agentContext":"\/jolokia",
         "maxObjects":"0",
         "debugMaxEntries":"100"
      },
      "info":{

      }
   },
   "timestamp":1513072097,
   "status":200
}
```

8. In order to deploy the configuration to Kubernetes, add the node port and the ports definition

```
spec:
  type: NodePort
  ports:
  - port: 7000
    protocol: TCP
    name: metrics

spec:
  template:
    spec:
        ports:
        - name: metrics:
          containerPort: 7000
```

With these steps your project is enabled to collect metrics.
