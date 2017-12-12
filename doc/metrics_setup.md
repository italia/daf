# Metrics Setup

In order to add metrics to the deployed micro-services we uses [jolokia-jmx](https://github.com/rhuss/jolokia).
Jolokia is a way to access JMX MBeans remotely. It is different from JSR-160 connectors in that it is an agent-based approach which uses JSON over HTTP for its communication in a REST-stylish way.

## Setup

### Publish sbt-jolokia to our local nexus

Clone the project [sbt-jolokia](https://github.com/fabiofumarola/sbt-jolokia)

```bash

$ git clone 
````
