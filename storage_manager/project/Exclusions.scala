import sbt.{ ExclusionRule, ModuleID }

object Exclusions {

  def hbase(module: ModuleID) = module
    .exclude("org.apache.thrift",        "thrift")
    .exclude("org.jruby",                "jruby-complete")
    .exclude("org.slf4j",                "slf4j-log4j12")
    .exclude("org.mortbay.jetty",        "jsp-2.1")
    .exclude("org.mortbay.jetty",        "jsp-api-2.1")
    .exclude("org.mortbay.jetty",        "servlet-api-2.5")
    .exclude("com.sun.jersey",           "jersey-core")
    .exclude("com.sun.jersey",           "jersey-json")
    .exclude("com.sun.jersey",           "jersey-server")
    .exclude("org.mortbay.jetty",        "jetty")
    .exclude("org.mortbay.jetty",        "jetty-util")
    .exclude("tomcat",                   "jasper-runtime")
    .exclude("tomcat",                   "jasper-compiler")
    .exclude("org.jboss.netty",          "netty")
    .exclude("commons-logging",          "commons-logging")
    .exclude("org.apache.xmlgraphics",   "batik-ext")
    .exclude("commons-collections",      "commons-collections")
    .exclude("xom",                      "xom")
    .exclude("commons-beanutils",        "commons-beanutils")
    .exclude("org.apache.logging.log4j", "log4j-slf4j-impl")

  def hadoop(module: ModuleID) = module
    .exclude("org.slf4j",                "slf4j-log4j12")
    .exclude("javax.servlet",            "servlet-api")
    .exclude("org.apache.logging.log4j", "log4j-slf4j-impl")
    .exclude("org.jboss.netty",          "netty")
    .excludeAll { ExclusionRule(organization = "javax.servlet") }

  def jetty(module: ModuleID) = module
    .exclude("org.mortbay.jetty", "jetty")
    .exclude("org.mortbay.jetty", "jetty-util")
    .exclude("org.mortbay.jetty", "jetty-sslengine")

  def slf4j(module: ModuleID) = module.exclude("org.slf4j", "*")

  def spark(module: ModuleID) = module
    .exclude("org.apache.hadoop",    "hadoop-client")
    .exclude("org.apache.hadoop",    "hadoop-yarn-client")
    .exclude("org.apache.hadoop",    "hadoop-yarn-api")
    .exclude("org.apache.hadoop",    "hadoop-yarn-common")
    .exclude("org.apache.hadoop",    "hadoop-yarn-server-common")
    .exclude("org.apache.hadoop",    "hadoop-yarn-server-web-proxy")
    .exclude("org.apache.zookeeper", "zookeeper")
    .exclude("commons-collections",  "commons-collections")
    .exclude("commons-beanutils",    "commons-beanutils")

}
