# Metrics Setup

In order to add metrics to the deployed micro-services we uses [jolokia-jmx](https://github.com/rhuss/jolokia).
Jolokia is a way to access JMX MBeans remotely. It is different from JSR-160 connectors in that it is an agent-based approach which uses JSON over HTTP for its communication in a REST-stylish way.

## Setup

### sbt-jolokia

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
`
