logLevel := Level.Warn

resolvers += Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns)

resolvers += "zalando-bintray"  at "https://dl.bintray.com/zalando/maven"

resolvers += "scalaz-bintray"   at "http://dl.bintray.com/scalaz/releases"

addSbtPlugin("com.typesafe.play" % "sbt-plugin"  % "2.5.14")

addSbtPlugin("de.zalando" % "sbt-api-first-hand" % "0.2.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M9")

addSbtPlugin("eu.unicredit" % "sbt-swagger-codegen" % "0.0.9")

// addSbtPlugin("org.wartremover" %% "sbt-wartremover" % "2.0.3")
// addSbtPlugin("org.wartremover" %% "sbt-wartremover" % "2.1.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.8.0")


addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

