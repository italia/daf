resolvers +=  Resolver.url("sbt-plugins", url("http://dl.bintray.com/gruggiero/sbt-plugins"))(Resolver.ivyStylePatterns)

//  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns)

resolvers += "zalando-bintray"  at "https://dl.bintray.com/zalando/maven"

resolvers += "scalaz-bintray"   at "http://dl.bintray.com/scalaz/releases"

resolvers += Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc" % "sbt-git-stamp" % "5.3.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin"  % "2.5.14")

addSbtPlugin("de.zalando" % "sbt-api-first-hand" % "0.2.4-daf")

addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.0")

addSbtPlugin("eu.unicredit" % "sbt-swagger-codegen" % "0.0.10" excludeAll(
  ExclusionRule(organization = "com.fasterxml.jackson.core"),
  ExclusionRule(organization = "com.fasterxml.jackson.dataformat"),
  ExclusionRule(organization = "com.fasterxml.jackson.datatype")))

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0-M9")

//addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.0.3")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "2.0.0")
