# Metrics Setup

To monitor our micro-services we can use:

1. [prometheus-client-java](https://github.com/prometheus/client_java) to export metrics in prometheus
2. [jolokia-jmx](https://github.com/rhuss/jolokia), to export metrics to influx-db and elastic

Unfortunately, prometheus accepts metrics data in [text-format](https://prometheus.io/docs/instrumenting/exposition_formats/). To use jolokia we need to plug a converter. Thus we prefer to allow metrics exposision in different formats.

> The minimal metrics exposition is **Prometheus**. Please use Jolokia only if you want to aggregate your metrics into elasticsearch.

## Prometheus java client

In order to add prometheus metrics exporter in a play application we need to:

1. add the library dependencies

```sbt

libraryDependencies ++= Seq(
  "io.prometheus" % "simpleclient" % "0.1.0",
  "io.prometheus" % "simpleclient_hotspot" % "0.1.0",
  "io.prometheus" % "simpleclient_common" % "0.1.0"
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

4. go to `http://localhost:9000/metrics` path on your browser to see the metrics.

Prometheus supports: Counters, Gauges, Histrograms that can be used to setup additional metrics as explained [here](https://github.com/prometheus/client_java#instrumenting).

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

7. navigate to `https://127.0.0.1:7000/jolokia/` and yould see a response like

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

8. In order to deploy the configuration to Kubernetes, add the node port and the ports definition.

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
